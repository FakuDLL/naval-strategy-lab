package dev.navalstrategylab.excepciones;

/** Una acción del juego fue rechazada sin modificar su estado. */
public class BatallaException extends Exception {
    public BatallaException(String mensaje) {
        super(mensaje);
    }
}
