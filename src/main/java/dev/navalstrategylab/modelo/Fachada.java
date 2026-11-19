package dev.navalstrategylab.modelo;

import java.util.List;
import java.util.Map;
import dev.navalstrategylab.excepciones.BatallaException;

/** Punto de acceso para presentadores; las reglas permanecen en sus expertos. */
public final class Fachada {
    private final SistemaBatallas sistema;

    public Fachada(SistemaBatallas sistema) { this.sistema = java.util.Objects.requireNonNull(sistema); }
    public void agregarObservador(dev.navalstrategylab.observador.Observador observador) { sistema.agregarObservador(observador); }
    public void quitarObservador(dev.navalstrategylab.observador.Observador observador) { sistema.quitarObservador(observador); }
    public List<String> listarTacticas() { return sistema.listarTacticas(); }
    public List<TipoBarco> listarTiposBarco() { return sistema.listarTiposBarco(); }
    public ResumenCampana consultarSala() { return sistema.consultarSala(); }
    public InstantaneaBatalla consultarBatalla(String nombre) throws BatallaException {
        return sistema.consultar(nombre);
    }
    public InstantaneaBatalla crearBatalla(String nombre, int ancho, int alto,
            Map<TipoBarco, Integer> cantidades, String tactica) throws BatallaException {
        return sistema.crear(nombre, ancho, alto, cantidades, tactica);
    }
    public InstantaneaBatalla colocarBarco(String nombre, long version, Coordenada proa, Orientacion orientacion)
            throws BatallaException { return sistema.colocar(nombre, version, proa, orientacion); }
    public InstantaneaBatalla iniciarBatalla(String nombre) throws BatallaException { return sistema.iniciar(nombre); }
    public InstantaneaBatalla disparar(String nombre, int ronda, Coordenada destino) throws BatallaException {
        return sistema.disparar(nombre, ronda, destino);
    }
}
