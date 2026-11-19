package dev.navalstrategylab.modelo;

public enum Orientacion {
    HORIZONTAL(0, 1),
    VERTICAL(1, 0);

    private final int avanceFila;
    private final int avanceColumna;

    Orientacion(int avanceFila, int avanceColumna) {
        this.avanceFila = avanceFila;
        this.avanceColumna = avanceColumna;
    }

    public Coordenada avanzar(Coordenada proa, int distancia) {
        return new Coordenada(proa.getFila() + avanceFila * distancia,
                proa.getColumna() + avanceColumna * distancia);
    }
}
