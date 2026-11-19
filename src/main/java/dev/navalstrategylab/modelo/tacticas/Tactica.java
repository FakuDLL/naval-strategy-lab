package dev.navalstrategylab.modelo.tacticas;

import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.Coordenada;

/** Elige usando solo información conocida, sin modificar el historial recibido. */
public interface Tactica {
    String getNombre();
    Coordenada elegirDisparo(InformacionDisparos informacion) throws BatallaException;
}
