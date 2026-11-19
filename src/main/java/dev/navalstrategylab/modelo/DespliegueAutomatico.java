package dev.navalstrategylab.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import dev.navalstrategylab.excepciones.BatallaException;

/** Busca una distribucion completa sin modificar un oceano de otra batalla. */
public final class DespliegueAutomatico {
    private static final int MAXIMO_INTENTOS = 100_000;
    private final Random azar;

    public DespliegueAutomatico() {
        this(new Random());
    }

    public DespliegueAutomatico(Random azar) {
        this.azar = java.util.Objects.requireNonNull(azar);
    }

    public Oceano crear(int ancho, int alto, ConfiguracionFlota flota) throws BatallaException {
        return completar(new Oceano(ancho, alto, flota));
    }

    Oceano completar(Oceano inicial) throws BatallaException {
        List<Posicion> posiciones = new ArrayList<>();
        for (int fila = 0; fila < inicial.getAlto(); fila++) {
            for (int columna = 0; columna < inicial.getAncho(); columna++) {
                for (Orientacion orientacion : Orientacion.values()) {
                    posiciones.add(new Posicion(new Coordenada(fila, columna), orientacion));
                }
            }
        }
        // El presupuesto pertenece a esta busqueda, nunca se comparte entre partidas.
        Presupuesto presupuesto = new Presupuesto();
        Oceano resultado = completar(inicial, posiciones, presupuesto);
        if (resultado == null) {
            throw new BatallaException("No se encontró una distribución válida para esta flota");
        }
        return resultado;
    }

    private Oceano completar(Oceano oceano, List<Posicion> posiciones, Presupuesto presupuesto)
            throws BatallaException {
        if (oceano.estaPreparado()) {
            return oceano;
        }
        List<Posicion> alternativas = new ArrayList<>(posiciones);
        Collections.shuffle(alternativas, azar);
        for (Posicion posicion : alternativas) {
            presupuesto.consumir();
            Oceano candidato = oceano.copiar();
            try {
                candidato.colocarSiguienteBarco(posicion.proa, posicion.orientacion);
            } catch (BatallaException colocacionInvalida) {
                continue;
            }
            Oceano completo = completar(candidato, posiciones, presupuesto);
            if (completo != null) {
                return completo;
            }
        }
        return null;
    }

    private static final class Posicion {
        private final Coordenada proa;
        private final Orientacion orientacion;

        Posicion(Coordenada proa, Orientacion orientacion) {
            this.proa = proa;
            this.orientacion = orientacion;
        }
    }

    private static final class Presupuesto {
        private int intentos;

        void consumir() throws BatallaException {
            if (++intentos > MAXIMO_INTENTOS) {
                throw new BatallaException("Se agotaron los intentos de distribución; vuelva a intentar");
            }
        }
    }
}
