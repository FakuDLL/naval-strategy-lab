package dev.navalstrategylab.modelo;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoordenadaTest {
    @Test
    void igualdadPorValorFuncionaParaBuscarCeldas() {
        HashSet<Coordenada> celdas = new HashSet<>();
        celdas.add(new Coordenada(2, 3));
        assertTrue(celdas.contains(new Coordenada(2, 3)));
        assertFalse(celdas.contains(new Coordenada(3, 2)));
        assertNotEquals(new Coordenada(2, 3), null);
        assertNotEquals(new Coordenada(2, 3), "(2, 3)");
    }

    @Test
    void adyacenciaNoConfundeSuperposicionNiDesbordaEnteros() {
        Coordenada origen = new Coordenada(0, 0);
        assertFalse(origen.esAdyacenteA(new Coordenada(0, 0)));
        assertTrue(origen.esAdyacenteA(new Coordenada(1, 1)));
        assertFalse(origen.esAdyacenteA(new Coordenada(0, 2)));
        assertFalse(new Coordenada(Integer.MIN_VALUE, 0)
                .esAdyacenteA(new Coordenada(Integer.MAX_VALUE, 0)));
    }
}
