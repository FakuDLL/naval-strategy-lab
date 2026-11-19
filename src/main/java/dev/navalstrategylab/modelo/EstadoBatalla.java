package dev.navalstrategylab.modelo;

import dev.navalstrategylab.excepciones.BatallaException;

abstract class EstadoBatalla {
    abstract SituacionBatalla getSituacion();

    abstract void colocar(Batalla batalla, Coordenada proa, Orientacion orientacion) throws BatallaException;

    abstract EstadoBatalla iniciar(Batalla batalla) throws BatallaException;

    abstract EstadoBatalla disparar(Batalla batalla, int ronda, Coordenada destino) throws BatallaException;
}
