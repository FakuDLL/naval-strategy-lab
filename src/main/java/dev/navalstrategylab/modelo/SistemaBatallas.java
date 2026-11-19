package dev.navalstrategylab.modelo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.tacticas.Tactica;

public final class SistemaBatallas extends dev.navalstrategylab.observador.Observable {
    private final Map<String, Batalla> batallas = new LinkedHashMap<>();
    private final CatalogoTacticas tacticas;
    private final DespliegueAutomatico despliegue;
    private final Random azar;

    public SistemaBatallas() {
        this(new CatalogoTacticas(), new DespliegueAutomatico(), new Random());
    }

    public SistemaBatallas(CatalogoTacticas tacticas, DespliegueAutomatico despliegue, Random azar) {
        this.tacticas = java.util.Objects.requireNonNull(tacticas);
        this.despliegue = java.util.Objects.requireNonNull(despliegue);
        this.azar = java.util.Objects.requireNonNull(azar);
    }

    public List<String> listarTacticas() { return tacticas.listar(); }
    public List<TipoBarco> listarTiposBarco() { return List.of(TipoBarco.values()); }

    public InstantaneaBatalla crear(String nombre, int ancho, int alto,
            Map<TipoBarco, Integer> cantidades, String nombreTactica) throws BatallaException {
        ConfiguracionFlota flota = new ConfiguracionFlota(cantidades);
        Oceano propio = new Oceano(ancho, alto, flota);
        Tactica tactica = tacticas.crear(nombreTactica);
        Oceano enemigo = new Oceano(ancho, alto, flota);
        InstantaneaBatalla creada;
        synchronized (this) {
            String lugar = nombre == null || nombre.isBlank() ? generarNombre() : nombre.strip();
            validarNombreLibre(lugar);
            Batalla batalla = new Batalla(lugar, propio, enemigo, tactica, despliegue);
            batallas.put(clave(lugar), batalla);
            creada = batalla.consultar();
        }
        avisar(creada.getNombre());
        return creada;
    }

    public ResumenCampana consultarSala() {
        List<Batalla> copia;
        synchronized (this) { copia = List.copyOf(batallas.values()); }
        List<InstantaneaBatalla> lecturas = new ArrayList<>();
        for (Batalla batalla : copia) { lecturas.add(batalla.consultar()); }
        return new ResumenCampana(lecturas);
    }

    public InstantaneaBatalla consultar(String nombre) throws BatallaException {
        return buscar(nombre).consultar();
    }
    public InstantaneaBatalla colocar(String nombre, long version, Coordenada proa, Orientacion orientacion)
            throws BatallaException {
        InstantaneaBatalla resultado = buscar(nombre).colocar(version, proa, orientacion);
        avisar(resultado.getNombre());
        return resultado;
    }
    public InstantaneaBatalla iniciar(String nombre) throws BatallaException {
        InstantaneaBatalla resultado = buscar(nombre).iniciar();
        avisar(resultado.getNombre());
        return resultado;
    }
    public InstantaneaBatalla disparar(String nombre, int ronda, Coordenada destino) throws BatallaException {
        InstantaneaBatalla resultado = buscar(nombre).disparar(ronda, destino);
        avisar(resultado.getNombre());
        return resultado;
    }

    private synchronized Batalla buscar(String nombre) throws BatallaException {
        Batalla batalla = nombre == null ? null : batallas.get(clave(nombre.strip()));
        if (batalla == null) { throw new BatallaException("La batalla indicada no existe"); }
        return batalla;
    }

    // Solo la precarga del modelo puede incorporar batallas construidas previamente.
    synchronized void incorporar(Batalla batalla) throws BatallaException {
        String nombre = batalla.consultar().getNombre();
        validarNombreLibre(nombre);
        batallas.put(clave(nombre), batalla);
    }

    private void validarNombreLibre(String nombre) throws BatallaException {
        if (batallas.containsKey(clave(nombre))) {
            throw new BatallaException("No se puede librar más de una batalla en el mismo lugar, cambie de nombre");
        }
    }

    private String generarNombre() throws BatallaException {
        for (int intento = 0; intento < 1000; intento++) {
            StringBuilder nombre = new StringBuilder();
            for (int letra = 0; letra < 6; letra++) nombre.append((char) ('A' + azar.nextInt(26)));
            if (!batallas.containsKey(clave(nombre.toString()))) return nombre.toString();
        }
        throw new BatallaException("No se pudo generar un nombre libre; indique un lugar de combate");
    }
    private String clave(String nombre) { return nombre.toLowerCase(Locale.ROOT); }
}
