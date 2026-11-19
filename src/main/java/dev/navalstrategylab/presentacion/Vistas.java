package dev.navalstrategylab.presentacion;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.*;
import dev.navalstrategylab.observador.Observable;
import dev.navalstrategylab.observador.Observador;

@Component
public class Vistas implements Observador {

    private final Map<String, Vista> vistas = new ConcurrentHashMap<>();
    private final Fachada fachada;
    private final Presentacion presentacion;
    private static final long INACTIVIDAD = 30 * 60 * 1000L;

    private static final class Vista {

        final String id = UUID.randomUUID().toString();
        String pantalla = "sala";
        String nombre;
        Orientacion orientacion = Orientacion.HORIZONTAL;
        long seleccionOrientacion;
        long entrada;
        long secuencia;
        long actividad = System.currentTimeMillis();
        SseEmitter emisor;
    }

    public Vistas(Fachada fachada, Presentacion presentacion) {
        this.fachada = fachada;
        this.presentacion = presentacion;
        fachada.agregarObservador(this);
    }

    public Commands abrir() {
        Vista v = new Vista();
        vistas.put(v.id, v);
        return Commands.uno("conectar", v.id);
    }

    /** Recupera la conexión sin ejecutar ni repetir acciones del juego. */
    public Commands recuperar(String id, String nombre) throws BatallaException {
        Vista vigente = id == null ? null : vistas.get(id);
        if (vigente != null) return Commands.uno("conectar", vigente.id);
        Vista nueva = new Vista();
        if (nombre != null && !nombre.isBlank()) {
            nueva.nombre = fachada.consultarBatalla(nombre).getNombre();
            nueva.pantalla = "batalla";
        }
        vistas.put(nueva.id, nueva);
        return Commands.uno("conectar", nueva.id);
    }

    public SseEmitter conectar(String id) throws BatallaException {
        Vista v = buscar(id);
        synchronized (v) {
            SseEmitter anterior = v.emisor;
            SseEmitter emisor = new SseEmitter(INACTIVIDAD);
            v.emisor = emisor;
            v.actividad = System.currentTimeMillis();
            if (anterior != null) anterior.complete();
            Runnable limpiar = () -> {
                synchronized (v) {
                    if (v.emisor == emisor) {
                        v.emisor = null;
                        v.actividad = System.currentTimeMillis();
                    }
                }
            };
            emisor.onCompletion(limpiar);
            emisor.onTimeout(() -> {
                limpiar.run();
                emisor.complete();
            });
            emisor.onError(e -> limpiar.run());
            enviar(v);
            return emisor;
        }
    }

    public Commands navegar(String id, String pantalla, String nombre) throws BatallaException {
        if (nombre != null) nombre = fachada.consultarBatalla(nombre).getNombre();
        Vista v = buscar(id);
        synchronized (v) {
            v.pantalla = pantalla;
            v.nombre = nombre;
            v.orientacion = Orientacion.HORIZONTAL;
            v.seleccionOrientacion = 0;
            v.entrada++;
            return comandos(v);
        }
    }

    public void orientar(String id, String nombre, long entrada, Orientacion orientacion, long seleccion)
        throws BatallaException {
        Vista v = buscar(id);
        synchronized (v) {
            exigir(v, nombre);
            if (v.entrada != entrada) throw new BatallaException(
                "La vista cambió; vuelva a intentar con la pantalla actual"
            );
            // Una selección anterior puede llegar después de la siguiente o del clic.
            if (seleccion >= v.seleccionOrientacion) {
                v.orientacion = orientacion;
                v.seleccionOrientacion = seleccion;
            }
        }
    }

    public void exigirBatalla(String id, String nombre) throws BatallaException {
        Vista v = buscar(id);
        synchronized (v) {
            exigir(v, nombre);
        }
    }

    private void exigir(Vista v, String nombre) throws BatallaException {
        if (!"batalla".equals(v.pantalla) || !Objects.equals(v.nombre, nombre)) throw new BatallaException(
            "La vista cambió; vuelva a entrar en la batalla"
        );
    }

    public Commands actualizar(String id) throws BatallaException {
        Vista v = buscar(id);
        synchronized (v) {
            return comandos(v);
        }
    }

    public void exigirVista(String id) throws BatallaException {
        Vista v = buscar(id);
        synchronized (v) {
            v.actividad = System.currentTimeMillis();
        }
    }

    public void cerrar(String id) {
        Vista v = vistas.remove(id);
        if (v != null) synchronized (v) {
            if (v.emisor != null) v.emisor.complete();
            v.emisor = null;
        }
    }

    private Vista buscar(String id) throws BatallaException {
        Vista v = id == null ? null : vistas.get(id);
        if (v == null) throw new BatallaException("La vista venció; vuelva a abrir la aplicación");
        return v;
    }

    private Commands comandos(Vista v) throws BatallaException {
        v.actividad = System.currentTimeMillis();
        List<Command> comandos = switch (v.pantalla) {
            case "nueva" -> presentacion.nueva();
            case "batalla" -> presentacion.batalla(v.id, fachada.consultarBatalla(v.nombre), v.orientacion);
            default -> presentacion.sala();
        };
        return Commands.uno("actualizar", Map.of(
            "vista", v.id,
            "secuencia", ++v.secuencia,
            "entrada", v.entrada,
            "seleccionOrientacion", v.seleccionOrientacion,
            "commands", comandos
        ));
    }

    private void enviar(Vista v) {
        if (v.emisor == null) return;
        try {
            v.emisor.send(comandos(v));
        } catch (IOException | IllegalStateException | BatallaException e) {
            SseEmitter fallido = v.emisor;
            v.emisor = null;
            if (fallido != null) fallido.complete();
        }
    }

    @Override
    public void actualizar(String nombre, Observable origen) {
        for (Vista v : vistas.values())
            synchronized (v) {
                if (
                    "sala".equals(v.pantalla) ||
                    ("batalla".equals(v.pantalla) && Objects.equals(v.nombre, nombre))
                ) enviar(v);
            }
    }

    @Scheduled(fixedDelay = 60000)
    public void limpiarInactivas() {
        long ahora = System.currentTimeMillis();
        for (Vista v : vistas.values())
            synchronized (v) {
                if (v.emisor == null && ahora - v.actividad > INACTIVIDAD) cerrar(v.id);
            }
    }

    @PreDestroy
    public void terminar() {
        fachada.quitarObservador(this);
        for (String id : vistas.keySet()) cerrar(id);
    }
}
