package dev.navalstrategylab.modelo;

import dev.navalstrategylab.excepciones.BatallaException;

final class EnCombate extends EstadoBatalla {
    @Override
    SituacionBatalla getSituacion() { return SituacionBatalla.EN_COMBATE; }

    @Override
    void colocar(Batalla batalla, Coordenada proa, Orientacion orientacion) throws BatallaException {
        throw new BatallaException("No puede disparar en aguas propias");
    }

    @Override
    EstadoBatalla iniciar(Batalla batalla) throws BatallaException {
        throw new BatallaException("La batalla ya fue iniciada");
    }

    @Override
    EstadoBatalla disparar(Batalla batalla, int ronda, Coordenada destino) throws BatallaException {
        SituacionBatalla resultado = batalla.hacerRonda(ronda, destino);
        if (resultado == SituacionBatalla.GANADA || resultado == SituacionBatalla.PERDIDA) {
            return new Finalizada(resultado);
        }
        return this;
    }
}
