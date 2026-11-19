package dev.navalstrategylab.observador;

@FunctionalInterface
public interface Observador {
    void actualizar(String batalla, Observable origen);
}
