package dev.navalstrategylab.modelo.tacticas;

import java.util.List;
import dev.navalstrategylab.modelo.Coordenada;
import dev.navalstrategylab.modelo.Disparo;

/** Conocimiento del atacante: no contiene barcos ni segmentos ocultos. */
public final class InformacionDisparos {
    private final int ancho;
    private final int alto;
    private final List<Disparo> disparos;

    public InformacionDisparos(int ancho, int alto, List<Disparo> disparos) {
        this.ancho = ancho;
        this.alto = alto;
        this.disparos = List.copyOf(disparos);
    }

    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }
    public List<Disparo> getDisparos() { return disparos; }

    public boolean estaDisponible(Coordenada coordenada) {
        if (coordenada == null || coordenada.getFila() < 0 || coordenada.getFila() >= alto
                || coordenada.getColumna() < 0 || coordenada.getColumna() >= ancho) {
            return false;
        }
        for (Disparo disparo : disparos) {
            if (disparo.getCoordenada().equals(coordenada)) { return false; }
        }
        return true;
    }
}
