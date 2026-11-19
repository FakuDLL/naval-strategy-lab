package dev.navalstrategylab.modelo;

import dev.navalstrategylab.excepciones.BatallaException;

final class EnPreparacion extends EstadoBatalla {
    @Override
    SituacionBatalla getSituacion() { return SituacionBatalla.EN_PREPARACION; }

    @Override
    void colocar(Batalla batalla, Coordenada proa, Orientacion orientacion) throws BatallaException {
        batalla.hacerColocacion(proa, orientacion);
    }

    @Override
    EstadoBatalla iniciar(Batalla batalla) throws BatallaException {
        batalla.prepararCombate();
        return new EnCombate();
    }

    @Override
    EstadoBatalla disparar(Batalla batalla, int ronda, Coordenada destino) throws BatallaException {
        throw new BatallaException("No puede ubicar barcos en aguas enemigas");
    }
}
