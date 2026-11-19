package dev.navalstrategylab.modelo;

import java.util.Objects;

/** Fila hacia arriba y columna hacia la derecha, desde (0, 0). */
public final class Coordenada {
    private final int fila;
    private final int columna;

    public Coordenada(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    public boolean esAdyacenteA(Coordenada otra) {
        long distanciaFilas = Math.abs((long) fila - otra.fila);
        long distanciaColumnas = Math.abs((long) columna - otra.columna);
        return distanciaFilas <= 1 && distanciaColumnas <= 1 && !equals(otra);
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Coordenada)) {
            return false;
        }
        Coordenada otra = (Coordenada) objeto;
        return fila == otra.fila && columna == otra.columna;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fila, columna);
    }

    @Override
    public String toString() {
        return "(" + fila + ", " + columna + ")";
    }
}
