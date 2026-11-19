package dev.navalstrategylab.modelo;

/** Catálogo fijo exigido por la letra; el tamaño no cambia el comportamiento. */
public enum TipoBarco {
    PORTAAVIONES("Portaaviones", 5),
    ACORAZADO("Acorazado", 4),
    CRUCERO("Crucero", 3),
    SUBMARINO("Submarino", 2),
    DESTRUCTOR("Destructor", 1);

    private final String nombre;
    private final int tamano;

    TipoBarco(String nombre, int tamano) {
        this.nombre = nombre;
        this.tamano = tamano;
    }

    public String getNombre() {
        return nombre;
    }

    public int getTamano() {
        return tamano;
    }
}
