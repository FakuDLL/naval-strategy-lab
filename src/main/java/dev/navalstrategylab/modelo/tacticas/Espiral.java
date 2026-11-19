package dev.navalstrategylab.modelo.tacticas;

import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.Coordenada;

public final class Espiral implements Tactica {
    @Override
    public String getNombre() {
        return "Espiral";
    }

    @Override
    public Coordenada elegirDisparo(InformacionDisparos informacion) throws BatallaException {
        // En dimensiones pares: central superior/izquierda, como la figura de la letra.
        int fila = informacion.getAlto() / 2;
        int columna = (informacion.getAncho() - 1) / 2;
        Coordenada centro = new Coordenada(fila, columna);
        if (informacion.estaDisponible(centro)) {
            return centro;
        }
        int visitadas = 1;
        int total = informacion.getAncho() * informacion.getAlto();
        int[] filas = {0, -1, 0, 1};
        int[] columnas = {1, 0, -1, 0};
        int direccion = 0;
        int longitud = 1;
        while (visitadas < total) {
            // Dos tramos de cada longitud: 1, 1, 2, 2, 3, 3...
            for (int tramo = 0; tramo < 2; tramo++) {
                for (int paso = 0; paso < longitud; paso++) {
                    fila += filas[direccion];
                    columna += columnas[direccion];
                    if (fila >= 0 && fila < informacion.getAlto()
                            && columna >= 0 && columna < informacion.getAncho()) {
                        visitadas++;
                        Coordenada candidata = new Coordenada(fila, columna);
                        if (informacion.estaDisponible(candidata)) {
                            return candidata;
                        }
                    }
                }
                direccion = (direccion + 1) % 4;
            }
            longitud++;
        }
        throw new BatallaException("No quedan celdas disponibles para disparar");
    }
}
