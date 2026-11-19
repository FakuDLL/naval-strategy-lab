package dev.navalstrategylab.modelo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.tacticas.*;

public final class CatalogoTacticas {
    private final Map<String, Supplier<Tactica>> fabricas = new LinkedHashMap<>();

    public CatalogoTacticas() {
        registrar("Secuencial", Secuencial::new);
        registrar("Espiral", Espiral::new);
        registrar("Caza y ataque", CazaYAtaque::new);
    }

    public synchronized void registrar(String nombre, Supplier<Tactica> fabrica) {
        if (nombre == null || nombre.isBlank() || fabrica == null || fabricas.containsKey(nombre)) {
            throw new IllegalArgumentException("La táctica debe tener nombre único y una fábrica");
        }
        fabricas.put(nombre, fabrica);
    }

    public synchronized List<String> listar() { return List.copyOf(fabricas.keySet()); }

    public synchronized Tactica crear(String nombre) throws BatallaException {
        Supplier<Tactica> fabrica = fabricas.get(nombre);
        if (fabrica == null) { throw new BatallaException("Seleccione una táctica de juego válida"); }
        Tactica tactica = fabrica.get();
        if (tactica == null || !nombre.equals(tactica.getNombre())) {
            throw new IllegalStateException("La fábrica debe crear la táctica registrada");
        }
        return tactica;
    }
}
