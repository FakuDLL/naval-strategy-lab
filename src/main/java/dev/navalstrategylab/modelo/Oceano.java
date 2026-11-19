package dev.navalstrategylab.modelo;

import java.util.ArrayList;
import java.util.List;
import dev.navalstrategylab.excepciones.BatallaException;

public class Oceano {
    private final int ancho;
    private final int alto;
    private final ConfiguracionFlota flota;
    private final List<TipoBarco> barcosPendientes;
    private final List<Barco> barcos = new ArrayList<>();
    private final List<Disparo> disparos = new ArrayList<>();

    public Oceano(int ancho, int alto, ConfiguracionFlota flota) throws BatallaException {
        validarDimension(ancho, "horizontal");
        validarDimension(alto, "vertical");
        if (flota == null) {
            throw new BatallaException("Debe indicar una flota");
        }
        flota.validarDensidad(ancho * alto);
        this.ancho = ancho;
        this.alto = alto;
        this.flota = flota;
        barcosPendientes = flota.crearListaBarcos();
    }

    public Barco colocarSiguienteBarco(Coordenada proa, Orientacion orientacion) throws BatallaException {
        if (barcosPendientes.isEmpty()) {
            throw new BatallaException("No quedan barcos por posicionar");
        }
        if (proa == null || !contiene(proa)) {
            throw new BatallaException("La proa debe estar dentro del océano");
        }
        if (orientacion == null) {
            throw new BatallaException("Debe indicar una orientación");
        }
        Barco candidato = new Barco(barcosPendientes.getFirst(), proa, orientacion);
        validarColocacion(candidato);
        // Ningún cambio se realiza hasta que se aceptó el barco completo.
        barcos.add(candidato);
        barcosPendientes.removeFirst();
        return candidato;
    }

    private void validarColocacion(Barco candidato) throws BatallaException {
        for (Coordenada celda : candidato.getCeldas()) {
            if (!contiene(celda)) {
                throw new BatallaException("El barco no puede quedar fuera de los límites del océano");
            }
        }
        for (Barco ubicado : barcos) {
            if (candidato.seSuperponeCon(ubicado)) {
                throw new BatallaException("El barco se superpone con otro barco");
            }
            if (candidato.tocaA(ubicado)) {
                throw new BatallaException("Debe quedar al menos una celda de agua entre los barcos");
            }
        }
    }

    private void validarDimension(int dimension, String eje) throws BatallaException {
        if (dimension < 6 || dimension > 12) {
            throw new BatallaException("La cantidad de celdas en dirección " + eje + " debe estar entre 6 y 12");
        }
    }

    public boolean contiene(Coordenada coordenada) {
        return coordenada != null && coordenada.getFila() >= 0 && coordenada.getFila() < alto
                && coordenada.getColumna() >= 0 && coordenada.getColumna() < ancho;
    }

    public int getAncho() {
        return ancho;
    }

    public int getAlto() {
        return alto;
    }

    public List<Barco> getBarcos() {
        return List.copyOf(barcos);
    }

    public List<TipoBarco> getBarcosPendientes() {
        return List.copyOf(barcosPendientes);
    }

    public boolean estaPreparado() {
        return barcosPendientes.isEmpty();
    }

    public Disparo recibirDisparo(Coordenada coordenada) throws BatallaException {
        validarDisparo(coordenada);
        Barco barco = buscarBarco(coordenada);
        ResultadoDisparo resultado = ResultadoDisparo.AGUA;
        int tamanoHundido = 0;
        if (barco != null) {
            resultado = barco.recibirImpacto(coordenada);
            if (resultado == ResultadoDisparo.HUNDIDO) {
                tamanoHundido = barco.getTipo().getTamano();
            }
        }
        Disparo disparo = new Disparo(coordenada, resultado, tamanoHundido);
        disparos.add(disparo);
        return disparo;
    }

    private void validarDisparo(Coordenada coordenada) throws BatallaException {
        if (!estaPreparado()) {
            throw new BatallaException("Debe posicionar todos sus barcos primero");
        }
        if (estaHundida()) {
            throw new BatallaException("La flota ya está hundida");
        }
        if (!contiene(coordenada)) {
            throw new BatallaException("El disparo debe estar dentro del océano");
        }
        if (yaFueDisparada(coordenada)) {
            throw new BatallaException("Ya se disparó a esa celda");
        }
    }

    private Barco buscarBarco(Coordenada coordenada) {
        for (Barco barco : barcos) {
            if (barco.ocupa(coordenada)) {
                return barco;
            }
        }
        return null;
    }

    public boolean yaFueDisparada(Coordenada coordenada) {
        for (Disparo disparo : disparos) {
            if (disparo.getCoordenada().equals(coordenada)) {
                return true;
            }
        }
        return false;
    }

    public List<Disparo> getDisparos() {
        return List.copyOf(disparos);
    }

    public List<Integer> getTamanosBarcosHundidos() {
        List<Integer> tamanos = new ArrayList<>();
        for (Disparo disparo : disparos) {
            if (disparo.getResultado() == ResultadoDisparo.HUNDIDO) {
                tamanos.add(disparo.getTamanoBarcoHundido());
            }
        }
        return List.copyOf(tamanos);
    }

    public int getCantidadPartes() {
        return flota.getCantidadCeldas();
    }

    public int getCantidadPartesSanas() {
        int sanas = 0;
        for (Barco barco : barcos) {
            sanas += barco.getCantidadPartesSanas();
        }
        for (TipoBarco pendiente : barcosPendientes) {
            sanas += pendiente.getTamano();
        }
        return sanas;
    }

    public boolean estaHundida() {
        return estaPreparado() && getCantidadPartesSanas() == 0;
    }

    private Oceano(Oceano original) {
        ancho = original.ancho;
        alto = original.alto;
        flota = original.flota;
        barcosPendientes = new ArrayList<>(original.barcosPendientes);
        for (Barco barco : original.barcos) {
            barcos.add(barco.copiar());
        }
        disparos.addAll(original.disparos);
    }

    Oceano copiar() {
        return new Oceano(this);
    }

    boolean tieneMismaConfiguracion(Oceano otro) {
        if (ancho != otro.ancho || alto != otro.alto) {
            return false;
        }
        for (TipoBarco tipo : TipoBarco.values()) {
            if (flota.getCantidad(tipo) != otro.flota.getCantidad(tipo)) {
                return false;
            }
        }
        return true;
    }
}
