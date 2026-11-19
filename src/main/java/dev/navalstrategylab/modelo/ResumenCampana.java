package dev.navalstrategylab.modelo;

import java.util.List;

/** Totales y filas calculados sobre la misma lectura de batallas. */
public final class ResumenCampana {
    private final List<InstantaneaBatalla> batallas;
    private final int ganadas;
    private final int perdidas;

    ResumenCampana(List<InstantaneaBatalla> batallas) {
        this.batallas = List.copyOf(batallas);
        int victorias = 0, derrotas = 0;
        for (InstantaneaBatalla batalla : batallas) {
            if (batalla.getSituacion() == SituacionBatalla.GANADA) victorias++;
            if (batalla.getSituacion() == SituacionBatalla.PERDIDA) derrotas++;
        }
        ganadas = victorias;
        perdidas = derrotas;
    }

    public List<InstantaneaBatalla> getBatallas() { return batallas; }
    public int getGanadas() { return ganadas; }
    public int getPerdidas() { return perdidas; }
    public int getTotal() { return batallas.size(); }
    public int getPorcentajeExito() {
        return batallas.isEmpty() ? 0 : (int) Math.round(100.0 * ganadas / batallas.size());
    }
}
