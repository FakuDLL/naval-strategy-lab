package dev.navalstrategylab.modelo;

import dev.navalstrategylab.excepciones.BatallaException;

final class Finalizada extends EstadoBatalla {
    private final SituacionBatalla situacion;

    Finalizada(SituacionBatalla situacion) {
        this.situacion = situacion;
    }

    @Override
    SituacionBatalla getSituacion() { return situacion; }

    @Override
    void colocar(Batalla batalla, Coordenada proa, Orientacion orientacion) throws BatallaException {
        throw new BatallaException("No se pueden colocar barcos en este estado de batalla");
    }

    @Override
    EstadoBatalla iniciar(Batalla batalla) throws BatallaException {
        throw new BatallaException("La batalla ya fue iniciada");
    }

    @Override
    EstadoBatalla disparar(Batalla batalla, int ronda, Coordenada destino) throws BatallaException {
        throw new BatallaException("Solo se puede disparar durante el combate");
    }
}
