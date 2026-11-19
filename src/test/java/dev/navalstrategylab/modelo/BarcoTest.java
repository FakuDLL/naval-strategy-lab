package dev.navalstrategylab.modelo;

import java.util.List;
import org.junit.jupiter.api.Test;
import dev.navalstrategylab.excepciones.BatallaException;
import static org.junit.jupiter.api.Assertions.*;
import static dev.navalstrategylab.modelo.Orientacion.HORIZONTAL;
import static dev.navalstrategylab.modelo.TipoBarco.SUBMARINO;

class BarcoTest {
    @Test
    void protegeSuIntegridadDeImpactosInvalidosYRepetidos() throws Exception {
        Barco barco = new Barco(SUBMARINO, new Coordenada(0, 0), HORIZONTAL);
        assertFalse(barco.ocupa(null));
        assertThrows(BatallaException.class, () -> barco.recibirImpacto(null));
        assertThrows(BatallaException.class, () -> barco.recibirImpacto(new Coordenada(5, 5)));
        assertEquals(2, barco.getCantidadPartesSanas());
        assertTrue(barco.getImpactos().isEmpty());
        barco.recibirImpacto(new Coordenada(0, 0));
        List<Coordenada> impactosAnteriores = barco.getImpactos();
        assertThrows(BatallaException.class, () -> barco.recibirImpacto(new Coordenada(0, 0)));
        assertEquals(1, barco.getCantidadPartesSanas());
        barco.recibirImpacto(new Coordenada(0, 1));
        assertTrue(barco.estaHundido());
        assertEquals(List.of(new Coordenada(0, 0)), impactosAnteriores);
        assertThrows(BatallaException.class, () -> barco.recibirImpacto(new Coordenada(0, 1)));
        assertEquals(0, barco.getCantidadPartesSanas());
    }
}
