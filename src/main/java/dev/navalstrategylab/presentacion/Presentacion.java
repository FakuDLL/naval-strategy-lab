package dev.navalstrategylab.presentacion;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;
import dev.navalstrategylab.modelo.*;

@Component
public class Presentacion {

    private final Fachada fachada;

    public Presentacion(Fachada fachada) {
        this.fachada = fachada;
    }

    public List<Command> sala() {
        ResumenCampana resumen = fachada.consultarSala();
        List<Map<String, Object>> filas = new ArrayList<>();
        for (InstantaneaBatalla b : resumen.getBatallas()) {
            filas.add(Map.of(
                "nombre", b.getNombre(),
                "dimensiones", b.getPropio().getAncho() + " × " + b.getPropio().getAlto(),
                "estado", estado(b.getSituacion()),
                "propia", integridad(b.getPropio()),
                "enemiga", integridad(b.getEnemigo())
            ));
        }
        return List.of(new Command("sala", Map.of(
            "ganadas", resumen.getGanadas(),
            "perdidas", resumen.getPerdidas(),
            "exito", resumen.getPorcentajeExito(),
            "batallas", filas
        )));
    }

    public List<Command> nueva() {
        List<Map<String, Object>> tipos = new ArrayList<>();
        for (TipoBarco tipo : fachada.listarTiposBarco())
            tipos.add(Map.of("id", tipo.name(), "nombre", tipo.getNombre(), "tamano", tipo.getTamano()));
        return List.of(new Command("nueva", Map.of("tacticas", fachada.listarTacticas(), "tipos", tipos)));
    }

    public List<Command> batalla(String vista, InstantaneaBatalla batalla, Orientacion orientacion) {
        List<Command> comandos = new ArrayList<>();
        comandos.add(datosBatalla(batalla, orientacion));
        comandos.add(tablero(vista, batalla));
        mostrarOceano(comandos, batalla.getPropio(), "mostrarAguasPropias");
        mostrarOceano(comandos, batalla.getEnemigo(), "mostrarAguasEnemigas");
        comandos.add(bajas(batalla.getEnemigo()));
        return comandos;
    }

    private Command datosBatalla(InstantaneaBatalla batalla, Orientacion orientacion) {
        boolean preparacion = batalla.getSituacion() == SituacionBatalla.EN_PREPARACION;
        List<String> pendientes = new ArrayList<>();
        for (TipoBarco tipo : batalla.getPropio().getPendientes()) pendientes.add(tipo.getNombre());
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", batalla.getNombre());
        datos.put("estado", estado(batalla.getSituacion()));
        datos.put("ronda", batalla.getRonda());
        datos.put("titulo", preparacion ? "Desplegando la Flota" : "Librar batalla");
        datos.put("preparacion", preparacion);
        datos.put("pendientes", pendientes);
        datos.put("orientacion", orientacion.name());
        datos.put("accion", accion(batalla, orientacion, pendientes));
        Map<String, String> acciones = new LinkedHashMap<>();
        for (Orientacion opcion : Orientacion.values()) acciones.put(opcion.name(), accion(batalla, opcion, pendientes));
        datos.put("acciones", acciones);
        datos.put("tactica", batalla.getTactica());
        return new Command("batalla", datos);
    }

    private String accion(InstantaneaBatalla batalla, Orientacion orientacion, List<String> pendientes) {
        if (
            batalla.getSituacion() == SituacionBatalla.GANADA ||
            batalla.getSituacion() == SituacionBatalla.PERDIDA
        ) return "Batalla finalizada";
        if (
            batalla.getSituacion() == SituacionBatalla.EN_COMBATE
        ) return "Seleccione una celda de aguas enemigas";
        if (pendientes.isEmpty()) return "Batalla preparada";
        return (
            "Posicione su " +
            pendientes.getFirst() +
            " en forma " +
            orientacion.name().toLowerCase(Locale.ROOT)
        );
    }

    private Command tablero(String vista, InstantaneaBatalla batalla) {
        String parametros = "?vista=" + codificar(vista) + "&nombre=" + codificar(batalla.getNombre());
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("numeroCeldasVerticales", batalla.getPropio().getAlto());
        datos.put("numeroCeldasHorizontales", batalla.getPropio().getAncho());
        datos.put(
            "escuchadorOceanoPropio",
            "/api/preparacion/celda" + parametros + "&version=" + batalla.getVersion()
        );
        datos.put(
            "escuchadorOceanoEnemigo",
            "/api/combate/disparar" + parametros + "&ronda=" + batalla.getRonda()
        );
        return new Command("inicializar", datos);
    }

    private Command bajas(InstantaneaOceano enemigo) {
        StringJoiner bajas = new StringJoiner(" ");
        for (int tamano : enemigo.getBajas()) bajas.add("{" + tamano + "}");
        return new Command("bajasEnemigas", bajas.length() == 0 ? "Ninguna" : bajas.toString());
    }

    private void mostrarOceano(List<Command> comandos, InstantaneaOceano oceano, String id) {
        for (Coordenada c : oceano.getCeldasBarcos()) comandos.add(celda(id, c, "barco"));
        for (Disparo d : oceano.getDisparos())
            comandos.add(
                celda(id, d.getCoordenada(), d.getResultado() == ResultadoDisparo.AGUA ? "agua" : "impacto")
            );
    }

    private Command celda(String id, Coordenada c, String valor) {
        return new Command(
            id,
            Map.of("coordenadaVertical", c.getFila(), "coordenadaHorizontal", c.getColumna(), "valor", valor)
        );
    }

    private String integridad(InstantaneaOceano o) {
        return o.getPartesSanas() + " / " + o.getPartesTotales();
    }

    private String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    private String estado(SituacionBatalla estado) {
        return switch (estado) {
            case EN_PREPARACION -> "En preparación";
            case EN_COMBATE -> "En combate";
            case GANADA -> "Ganada";
            case PERDIDA -> "Perdida";
        };
    }
}
