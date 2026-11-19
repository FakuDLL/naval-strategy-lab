package dev.navalstrategylab.modelo;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.tacticas.*;
import static org.junit.jupiter.api.Assertions.*;

class SecuencialTest {
    @TestFactory List<DynamicTest> recorreCadaRectanguloSinRepetir() {
        List<DynamicTest> pruebas = new ArrayList<>();
        for (int a = 6; a <= 12; a++) for (int h = 6; h <= 12; h++) {
            final int ancho = a, alto = h;
            pruebas.add(DynamicTest.dynamicTest(ancho + "x" + alto, () -> {
                List<Disparo> historial = new ArrayList<>();
                Secuencial tactica = new Secuencial();
                for (int fila = 0; fila < alto; fila++) for (int columna = 0; columna < ancho; columna++) {
                    InformacionDisparos info = new InformacionDisparos(ancho, alto, historial);
                    Coordenada elegida = tactica.elegirDisparo(info);
                    assertEquals(new Coordenada(fila, columna), elegida);
                    assertTrue(info.estaDisponible(elegida));
                    historial.add(new Disparo(elegida, ResultadoDisparo.AGUA, 0));
                    assertEquals(historial.size() - 1, info.getDisparos().size());
                }
                assertThrows(BatallaException.class, () -> tactica.elegirDisparo(new InformacionDisparos(ancho, alto, historial)));
            }));
        }
        return pruebas;
    }
}
