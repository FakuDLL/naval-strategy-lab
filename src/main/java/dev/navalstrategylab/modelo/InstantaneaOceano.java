package dev.navalstrategylab.modelo;

import java.util.ArrayList;
import java.util.List;

/** Lectura inmutable del océano con la visibilidad decidida por la batalla. */
public final class InstantaneaOceano {
    private final int ancho;
    private final int alto;
    private final int partesSanas;
    private final int partesTotales;
    private final List<Coordenada> celdasBarcos;
    private final List<TipoBarco> pendientes;
    private final List<Disparo> disparos;
    private final List<Integer> bajas;

    InstantaneaOceano(Oceano oceano, boolean revelar) {
        ancho = oceano.getAncho();
        alto = oceano.getAlto();
        partesSanas = oceano.getCantidadPartesSanas();
        partesTotales = oceano.getCantidadPartes();
        List<Coordenada> celdas = new ArrayList<>();
        if (revelar) {
            for (Barco barco : oceano.getBarcos()) { celdas.addAll(barco.getCeldas()); }
        }
        celdasBarcos = List.copyOf(celdas);
        pendientes = revelar ? oceano.getBarcosPendientes() : List.of();
        disparos = oceano.getDisparos();
        bajas = oceano.getTamanosBarcosHundidos();
    }

    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }
    public int getPartesSanas() { return partesSanas; }
    public int getPartesTotales() { return partesTotales; }
    public List<Coordenada> getCeldasBarcos() { return celdasBarcos; }
    public List<TipoBarco> getPendientes() { return pendientes; }
    public List<Disparo> getDisparos() { return disparos; }
    public List<Integer> getBajas() { return bajas; }
}
