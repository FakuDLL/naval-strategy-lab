package dev.navalstrategylab.observador;

import java.util.ArrayList;
import java.util.List;

public class Observable {
    private final List<Observador> observadores = new ArrayList<>();

    public void agregarObservador(Observador observador) {
        synchronized (observadores) {
            if (!observadores.contains(observador)) {
                observadores.add(observador);
            }
        }
    }

    public void quitarObservador(Observador observador) {
        synchronized (observadores) {
            observadores.remove(observador);
        }
    }

    protected void avisar(String batalla) {
        List<Observador> copia;
        synchronized (observadores) {
            copia = new ArrayList<>(observadores);
        }
        for (Observador observador : copia) {
            try {
                observador.actualizar(batalla, this);
            } catch (RuntimeException e) {
                System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING, "No se pudo actualizar una vista", e);
            }
        }
    }
}
