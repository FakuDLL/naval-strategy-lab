package dev.navalstrategylab.modelo;

/** Resultado histórico: no cambia si el barco recibe impactos posteriores. */
public final class Disparo {
    private final Coordenada coordenada;
    private final ResultadoDisparo resultado;
    private final int tamanoBarcoHundido;

    Disparo(Coordenada coordenada, ResultadoDisparo resultado, int tamanoBarcoHundido) {
        this.coordenada = coordenada;
        this.resultado = resultado;
        this.tamanoBarcoHundido = tamanoBarcoHundido;
    }

    public Coordenada getCoordenada() {
        return coordenada;
    }

    public ResultadoDisparo getResultado() {
        return resultado;
    }

    /** Cero si el disparo no hundió un barco. No revela su tamaño al impactarlo. */
    public int getTamanoBarcoHundido() {
        return tamanoBarcoHundido;
    }
}
