package dev.navalstrategylab.modelo;

import java.util.ArrayList;
import java.util.List;
import dev.navalstrategylab.excepciones.BatallaException;

/** Su posición se fija al colocarlo; no permite moverlo desde una vista. */
public final class Barco {
    private final TipoBarco tipo;
    private final List<Coordenada> celdas;
    private final List<Coordenada> impactos = new ArrayList<>();

    Barco(TipoBarco tipo, Coordenada proa, Orientacion orientacion) {
        this.tipo = tipo;
        List<Coordenada> posicion = new ArrayList<>();
        for (int segmento = 0; segmento < tipo.getTamano(); segmento++) {
            posicion.add(orientacion.avanzar(proa, segmento));
        }
        celdas = List.copyOf(posicion);
    }

    public TipoBarco getTipo() {
        return tipo;
    }

    private Barco(Barco original) {
        tipo = original.tipo;
        celdas = original.celdas;
        impactos.addAll(original.impactos);
    }

    Barco copiar() {
        return new Barco(this);
    }

    public List<Coordenada> getCeldas() {
        return celdas;
    }

    public boolean ocupa(Coordenada coordenada) {
        return coordenada != null && celdas.contains(coordenada);
    }

    ResultadoDisparo recibirImpacto(Coordenada coordenada) throws BatallaException {
        if (coordenada == null || !ocupa(coordenada)) {
            throw new BatallaException("El impacto debe corresponder a una celda del barco");
        }
        if (impactos.contains(coordenada)) {
            throw new BatallaException("Esta parte del barco ya fue alcanzada");
        }
        impactos.add(coordenada);
        return estaHundido() ? ResultadoDisparo.HUNDIDO : ResultadoDisparo.IMPACTO;
    }

    public int getCantidadPartesSanas() {
        return tipo.getTamano() - impactos.size();
    }

    public boolean estaHundido() {
        return getCantidadPartesSanas() == 0;
    }

    public List<Coordenada> getImpactos() {
        return List.copyOf(impactos);
    }

    public boolean seSuperponeCon(Barco otro) {
        for (Coordenada celda : celdas) {
            if (otro.celdas.contains(celda)) {
                return true;
            }
        }
        return false;
    }

    public boolean tocaA(Barco otro) {
        for (Coordenada celda : celdas) {
            for (Coordenada otra : otro.celdas) {
                if (celda.esAdyacenteA(otra)) {
                    return true;
                }
            }
        }
        return false;
    }
}
