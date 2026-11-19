package dev.navalstrategylab.modelo;

import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import dev.navalstrategylab.excepciones.BatallaException;
import static org.junit.jupiter.api.Assertions.*;
import static dev.navalstrategylab.modelo.TipoBarco.*;

class DespliegueAutomaticoTest {
    @TestFactory List<DynamicTest> distribuyeFlotasEnTodosLosRectangulos() {
        List<DynamicTest> casos = new ArrayList<>();
        for (int a = 6; a <= 12; a++) for (int h = 6; h <= 12; h++) {
            final int ancho = a, alto = h;
            casos.add(DynamicTest.dynamicTest(ancho + "x" + alto, () -> {
                for (int semilla = 0; semilla < 10; semilla++) {
                    final long semillaActual = ancho * 1000L + alto * 100L + semilla;
                    Map<TipoBarco, Integer> cantidades = new EnumMap<>(TipoBarco.class);
                    cantidades.put(PORTAAVIONES, 1);
                    cantidades.put(SUBMARINO, 1);
                    cantidades.put(DESTRUCTOR, 1);
                    // Agregar barcos hasta acercarse a la densidad maxima permitida.
                    int celdas = 8;
                    for (TipoBarco tipo : TipoBarco.values()) {
                        while (cantidades.getOrDefault(tipo, 0) < 6 && (celdas + tipo.getTamano()) * 4 < ancho * alto) {
                            cantidades.merge(tipo, 1, Integer::sum);
                            celdas += tipo.getTamano();
                        }
                    }
                    ConfiguracionFlota flota = new ConfiguracionFlota(cantidades);
                    Oceano o = assertTimeoutPreemptively(Duration.ofSeconds(5),
                            () -> new DespliegueAutomatico(new Random(semillaActual)).crear(ancho, alto, flota));
                    verificar(o, cantidades);
                }
            }));
        }
        return casos;
    }
    private void verificar(Oceano o, Map<TipoBarco, Integer> cantidades) {
        assertTrue(o.estaPreparado());
        assertTrue(o.getDisparos().isEmpty());
        assertEquals(o.getCantidadPartes(), o.getCantidadPartesSanas());
        for (TipoBarco tipo : TipoBarco.values()) {
            assertEquals(cantidades.getOrDefault(tipo, 0).longValue(),
                    o.getBarcos().stream().filter(b -> b.getTipo() == tipo).count());
        }
        List<Barco> barcos = o.getBarcos();
        for (int i = 0; i < barcos.size(); i++) {
            for (Coordenada celda : barcos.get(i).getCeldas()) assertTrue(o.contiene(celda));
            for (int j = i + 1; j < barcos.size(); j++) {
                for (Coordenada a : barcos.get(i).getCeldas()) for (Coordenada b : barcos.get(j).getCeldas()) {
                    assertTrue(Math.abs(a.getFila() - b.getFila()) > 1 || Math.abs(a.getColumna() - b.getColumna()) > 1);
                }
            }
        }
    }
    @Test void semillasDistintasProducenVariedadSinCompartirDanio() throws Exception {
        ConfiguracionFlota f = new ConfiguracionFlota(Map.of(CRUCERO, 1, DESTRUCTOR, 1));
        Set<List<Coordenada>> distribuciones = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            Oceano o = new DespliegueAutomatico(new Random(i)).crear(6, 7, f);
            distribuciones.add(o.getBarcos().stream().flatMap(b -> b.getCeldas().stream()).toList());
        }
        assertTrue(distribuciones.size() > 1);
        DespliegueAutomatico generador = new DespliegueAutomatico(new Random(10));
        Oceano a = generador.crear(6, 7, f), b = generador.crear(6, 7, f);
        a.recibirDisparo(a.getBarcos().getFirst().getCeldas().getFirst());
        assertEquals(4, b.getCantidadPartesSanas());
        assertTrue(b.getDisparos().isEmpty());
    }
    @Test void validacionesOriginalesSeConservan() throws Exception {
        DespliegueAutomatico g = new DespliegueAutomatico();
        ConfiguracionFlota f = new ConfiguracionFlota(Map.of(PORTAAVIONES, 2));
        assertThrows(BatallaException.class, () -> g.crear(6, 6, f));
        assertThrows(BatallaException.class, () -> g.crear(5, 12, f));
        assertThrows(BatallaException.class, () -> g.crear(6, 6, null));
    }
}
