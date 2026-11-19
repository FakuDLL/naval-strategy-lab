package dev.navalstrategylab.modelo.tacticas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.Coordenada;
import dev.navalstrategylab.modelo.Disparo;
import dev.navalstrategylab.modelo.ResultadoDisparo;

public final class CazaYAtaque implements Tactica {
    private final Random azar;

    public CazaYAtaque() {
        this(new Random());
    }

    public CazaYAtaque(Random azar) {
        this.azar = Objects.requireNonNull(azar);
    }

    @Override
    public String getNombre() {
        return "Caza y ataque";
    }

    @Override
    public Coordenada elegirDisparo(InformacionDisparos informacion) throws BatallaException {
        List<Coordenada> impactos = impactosDelAtaque(informacion);
        if (impactos.isEmpty()) {
            return cazar(informacion);
        }
        return atacar(informacion, impactos);
    }

    private List<Coordenada> impactosDelAtaque(InformacionDisparos informacion) {
        List<Coordenada> impactos = new ArrayList<>();
        for (Disparo disparo : informacion.getDisparos()) {
            if (disparo.getResultado() == ResultadoDisparo.HUNDIDO) {
                impactos.clear();
            } else if (disparo.getResultado() == ResultadoDisparo.IMPACTO) {
                impactos.add(disparo.getCoordenada());
            }
        }
        // Esta tactica termina cada barco antes de volver a cazar otro.
        return impactos;
    }

    private Coordenada cazar(InformacionDisparos informacion) throws BatallaException {
        List<Coordenada> disponibles = new ArrayList<>();
        for (int fila = 0; fila < informacion.getAlto(); fila++) {
            for (int columna = 0; columna < informacion.getAncho(); columna++) {
                Coordenada candidata = new Coordenada(fila, columna);
                if (informacion.estaDisponible(candidata)) {
                    disponibles.add(candidata);
                }
            }
        }
        if (disponibles.isEmpty()) {
            throw new BatallaException("No quedan celdas disponibles para disparar");
        }
        return disponibles.get(azar.nextInt(disponibles.size()));
    }

    private Coordenada atacar(InformacionDisparos informacion, List<Coordenada> impactos)
            throws BatallaException {
        Coordenada primero = impactos.getFirst();
        List<Coordenada> candidatas = new ArrayList<>();
        if (impactos.size() == 1) {
            candidatas.add(new Coordenada(primero.getFila(), primero.getColumna() + 1));
            candidatas.add(new Coordenada(primero.getFila() - 1, primero.getColumna()));
            candidatas.add(new Coordenada(primero.getFila(), primero.getColumna() - 1));
            candidatas.add(new Coordenada(primero.getFila() + 1, primero.getColumna()));
        } else {
            boolean horizontal = primero.getFila() == impactos.get(1).getFila();
            int minimo = Integer.MAX_VALUE;
            int maximo = Integer.MIN_VALUE;
            for (Coordenada impacto : impactos) {
                int posicion = horizontal ? impacto.getColumna() : impacto.getFila();
                minimo = Math.min(minimo, posicion);
                maximo = Math.max(maximo, posicion);
            }
            if (horizontal) {
                candidatas.add(new Coordenada(primero.getFila(), maximo + 1));
                candidatas.add(new Coordenada(primero.getFila(), minimo - 1));
            } else {
                candidatas.add(new Coordenada(maximo + 1, primero.getColumna()));
                candidatas.add(new Coordenada(minimo - 1, primero.getColumna()));
            }
        }
        for (Coordenada candidata : candidatas) {
            if (informacion.estaDisponible(candidata)) {
                return candidata;
            }
        }
        throw new BatallaException("No hay posiciones válidas para continuar el ataque");
    }
}
