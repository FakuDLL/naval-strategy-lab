package dev.navalstrategylab.modelo.tacticas;

import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.Coordenada;

public final class Secuencial implements Tactica {
    @Override
    public String getNombre() { return "Secuencial"; }

    @Override
    public Coordenada elegirDisparo(InformacionDisparos informacion) throws BatallaException {
        for (int fila = 0; fila < informacion.getAlto(); fila++) {
            for (int columna = 0; columna < informacion.getAncho(); columna++) {
                Coordenada coordenada = new Coordenada(fila, columna);
                if (informacion.estaDisponible(coordenada)) { return coordenada; }
            }
        }
        throw new BatallaException("No quedan celdas disponibles para disparar");
    }
}
