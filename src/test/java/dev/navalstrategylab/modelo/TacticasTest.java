package dev.navalstrategylab.modelo;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.tacticas.*;
import static org.junit.jupiter.api.Assertions.*;

class TacticasTest {
    private InformacionDisparos informacion(int ancho, int alto, List<Disparo> disparos) {
        return new InformacionDisparos(ancho, alto, disparos);
    }
    private Disparo disparo(int fila, int columna, ResultadoDisparo resultado) {
        return new Disparo(new Coordenada(fila, columna), resultado,
                resultado == ResultadoDisparo.HUNDIDO ? 3 : 0);
    }

    @Test void espiralCoincideConFiguraDeLaLetra() throws Exception {
        int[][] figura = {{4,2},{4,3},{3,3},{3,2},{3,1},{4,1},{5,1},{5,2},{5,3},{5,4},
                {4,4},{3,4},{2,4},{2,3},{2,2},{2,1},{2,0},{3,0},{4,0},{5,0},{6,0},{6,1},{6,2},{6,3},{6,4},{6,5},{5,5},{4,5},{3,5}};
        List<Disparo> historial = new ArrayList<>();
        Espiral espiral = new Espiral();
        for (int[] celda : figura) {
            Coordenada elegida = espiral.elegirDisparo(informacion(6, 8, historial));
            assertEquals(new Coordenada(celda[0], celda[1]), elegida);
            historial.add(disparo(celda[0], celda[1], ResultadoDisparo.AGUA));
        }
    }
    @TestFactory List<DynamicTest> espiralCubreCadaRectanguloSinSalirNiRepetir() {
        List<DynamicTest> casos = new ArrayList<>();
        for (int a = 6; a <= 12; a++) for (int h = 6; h <= 12; h++) {
            final int ancho = a, alto = h;
            casos.add(DynamicTest.dynamicTest(ancho + "x" + alto, () -> {
                Espiral e = new Espiral();
                List<Disparo> historial = new ArrayList<>();
                Set<Coordenada> visitadas = new HashSet<>();
                for (int n = 0; n < ancho * alto; n++) {
                    Coordenada c = e.elegirDisparo(informacion(ancho, alto, historial));
                    assertTrue(c.getFila() >= 0 && c.getFila() < alto);
                    assertTrue(c.getColumna() >= 0 && c.getColumna() < ancho);
                    assertTrue(visitadas.add(c));
                    historial.add(new Disparo(c, ResultadoDisparo.AGUA, 0));
                }
                assertEquals(ancho * alto, visitadas.size());
                assertThrows(BatallaException.class, () -> e.elegirDisparo(informacion(ancho, alto, historial)));
            }));
        }
        return casos;
    }
    @Test void ataqueDescartaAguaYDescubreOrientacionVertical() throws Exception {
        CazaYAtaque caza = new CazaYAtaque(new Random(1));
        List<Disparo> historial = new ArrayList<>();
        historial.add(disparo(3, 3, ResultadoDisparo.IMPACTO));
        assertEquals(new Coordenada(3, 4), caza.elegirDisparo(informacion(6, 6, historial)));
        historial.add(disparo(3, 4, ResultadoDisparo.AGUA));
        assertEquals(new Coordenada(2, 3), caza.elegirDisparo(informacion(6, 6, historial)));
        historial.add(disparo(2, 3, ResultadoDisparo.IMPACTO));
        assertEquals(new Coordenada(4, 3), caza.elegirDisparo(informacion(6, 6, historial)));
        historial.add(disparo(4, 3, ResultadoDisparo.AGUA));
        assertEquals(new Coordenada(1, 3), caza.elegirDisparo(informacion(6, 6, historial)));
        assertEquals(4, historial.size());
    }
    @Test void ataqueHorizontalRetomaElOtroExtremo() throws Exception {
        CazaYAtaque caza = new CazaYAtaque();
        List<Disparo> historial = List.of(disparo(3, 3, ResultadoDisparo.IMPACTO),
                disparo(3, 4, ResultadoDisparo.IMPACTO), disparo(3, 5, ResultadoDisparo.AGUA));
        assertEquals(new Coordenada(3, 2), caza.elegirDisparo(informacion(6, 6, historial)));
    }
    @Test void hundimientoVuelveACazaYNingunaPartidaContaminaOtra() throws Exception {
        long semilla = 42;
        CazaYAtaque caza = new CazaYAtaque(new Random(semilla));
        caza.elegirDisparo(informacion(6, 6, List.of(disparo(3, 3, ResultadoDisparo.IMPACTO))));
        List<Disparo> hundido = List.of(disparo(3, 1, ResultadoDisparo.IMPACTO),
                disparo(3, 2, ResultadoDisparo.IMPACTO), disparo(3, 3, ResultadoDisparo.HUNDIDO));
        List<Coordenada> disponibles = new ArrayList<>();
        InformacionDisparos info = informacion(6, 6, hundido);
        for (int f = 0; f < 6; f++) for (int c = 0; c < 6; c++) {
            Coordenada celda = new Coordenada(f, c);
            if (info.estaDisponible(celda)) disponibles.add(celda);
        }
        assertEquals(disponibles.get(new Random(semilla).nextInt(disponibles.size())), caza.elegirDisparo(info));
        // Una historia distinta conserva su propio objetivo, aunque se use la misma estrategia.
        assertEquals(new Coordenada(0, 1), caza.elegirDisparo(informacion(6, 6,
                List.of(disparo(0, 0, ResultadoDisparo.IMPACTO)))));
    }
    @Test void cazaSoloEligeCeldasLibresYDetectaAgotamiento() throws Exception {
        List<Disparo> historial = new ArrayList<>();
        for (int f = 0; f < 6; f++) for (int c = 0; c < 6; c++) {
            if (f != 5 || c != 5) historial.add(disparo(f, c, ResultadoDisparo.AGUA));
        }
        CazaYAtaque caza = new CazaYAtaque();
        assertEquals(new Coordenada(5, 5), caza.elegirDisparo(informacion(6, 6, historial)));
        historial.add(disparo(5, 5, ResultadoDisparo.AGUA));
        assertThrows(BatallaException.class, () -> caza.elegirDisparo(informacion(6, 6, historial)));
    }
    @ParameterizedTest @EnumSource(TipoBarco.class)
    void ataqueHundeDesdeCualquierSegmentoInclusoEnBordes(TipoBarco tipo) throws Exception {
        for (Orientacion orientacion : Orientacion.values()) {
            for (int segmento = 0; segmento < tipo.getTamano(); segmento++) {
                for (int borde : new int[] {0, 5}) {
                    Oceano o = new Oceano(6, 6, new ConfiguracionFlota(Map.of(tipo, 1)));
                    Coordenada proa = orientacion == Orientacion.HORIZONTAL ? new Coordenada(borde, 0) : new Coordenada(0, borde);
                    Barco barco = o.colocarSiguienteBarco(proa, orientacion);
                    o.recibirDisparo(barco.getCeldas().get(segmento));
                    CazaYAtaque tactica = new CazaYAtaque(new Random(1));
                    int disparos = 1;
                    while (!o.estaHundida() && disparos < 36) {
                        o.recibirDisparo(tactica.elegirDisparo(informacion(6, 6, o.getDisparos())));
                        disparos++;
                    }
                    assertTrue(o.estaHundida());
                    assertTrue(disparos <= tipo.getTamano() + 4, "El ataque debe mantenerse en torno al barco");
                }
            }
        }
    }
    @TestFactory List<DynamicTest> tacticasHundenFlotasRealesEnTodosLosRectangulos() {
        List<DynamicTest> casos = new ArrayList<>();
        for (int a = 6; a <= 12; a++) for (int h = 6; h <= 12; h++) {
            final int ancho = a, alto = h;
            for (boolean caza : new boolean[] {false, true}) {
                casos.add(DynamicTest.dynamicTest((caza ? "Caza " : "Espiral ") + ancho + "x" + alto, () -> {
                    for (int semilla = 0; semilla < 5; semilla++) {
                        Oceano o = new DespliegueAutomatico(new Random(semilla)).crear(ancho, alto,
                                new ConfiguracionFlota(Map.of(TipoBarco.PORTAAVIONES, 1, TipoBarco.SUBMARINO, 1, TipoBarco.DESTRUCTOR, 1)));
                        Tactica tactica = caza ? new CazaYAtaque(new Random(semilla + 100)) : new Espiral();
                        int disparos = 0;
                        while (!o.estaHundida() && disparos < ancho * alto) {
                            Coordenada c = tactica.elegirDisparo(informacion(ancho, alto, o.getDisparos()));
                            assertFalse(o.yaFueDisparada(c));
                            o.recibirDisparo(c);
                            disparos++;
                        }
                        assertTrue(o.estaHundida());
                        assertEquals(List.of(1, 2, 5), o.getTamanosBarcosHundidos().stream().sorted().toList());
                    }
                }));
            }
        }
        return casos;
    }
}
