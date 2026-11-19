package dev.navalstrategylab.modelo;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import dev.navalstrategylab.excepciones.BatallaException;

/** Composición inmutable, reutilizable para crear dos océanos independientes. */
public final class ConfiguracionFlota {
    private final Map<TipoBarco, Integer> cantidades = new EnumMap<>(TipoBarco.class);

    public ConfiguracionFlota(Map<TipoBarco, Integer> cantidades) throws BatallaException {
        if (cantidades == null) {
            throw new BatallaException("Debe indicar una configuración de flota válida");
        }
        for (TipoBarco tipo : cantidades.keySet()) {
            if (tipo == null) {
                throw new BatallaException("Debe indicar un tipo de barco válido");
            }
        }
        for (TipoBarco tipo : TipoBarco.values()) {
            Integer cantidad = cantidades.getOrDefault(tipo, 0);
            if (cantidad == null || cantidad < 0 || cantidad > 6) {
                throw new BatallaException("La cantidad de " + tipo.getNombre() + " debe estar entre 0 y 6");
            }
            this.cantidades.put(tipo, cantidad);
        }
        if (getCantidadCeldas() == 0) {
            throw new BatallaException("La flota debe tener al menos un barco");
        }
    }

    public int getCantidad(TipoBarco tipo) {
        return cantidades.getOrDefault(tipo, 0);
    }

    public int getCantidadCeldas() {
        int total = 0;
        for (TipoBarco tipo : TipoBarco.values()) {
            total += tipo.getTamano() * getCantidad(tipo);
        }
        return total;
    }

    public void validarDensidad(int celdasOceano) throws BatallaException {
        // Comparación entera estricta: exactamente 25% también se rechaza.
        if (4L * getCantidadCeldas() >= celdasOceano) {
            throw new BatallaException("La flota debe ocupar menos del 25% de las celdas del océano");
        }
    }

    public List<TipoBarco> crearListaBarcos() {
        List<TipoBarco> barcos = new ArrayList<>();
        for (TipoBarco tipo : TipoBarco.values()) {
            for (int unidad = 0; unidad < getCantidad(tipo); unidad++) {
                barcos.add(tipo);
            }
        }
        barcos.sort((primero, segundo) -> Integer.compare(segundo.getTamano(), primero.getTamano()));
        return barcos;
    }
}
