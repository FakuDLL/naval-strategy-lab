package dev.navalstrategylab.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import dev.navalstrategylab.excepciones.BatallaException;
import static org.junit.jupiter.api.Assertions.*;
import static dev.navalstrategylab.modelo.Orientacion.*;
import static dev.navalstrategylab.modelo.ResultadoDisparo.*;
import static dev.navalstrategylab.modelo.TipoBarco.*;

class DisparosTest {
    @Test
    void aguaQuedaRegistradaSinDanarLaFlota() throws Exception {
        Oceano oceano = preparado(Map.of(SUBMARINO, 1));
        Disparo disparo = oceano.recibirDisparo(new Coordenada(5, 5));
        assertEquals(AGUA, disparo.getResultado());
        assertEquals(new Coordenada(5, 5), disparo.getCoordenada());
        assertEquals(0, disparo.getTamanoBarcoHundido());
        assertEquals(List.of(disparo), oceano.getDisparos());
        assertTrue(oceano.yaFueDisparada(new Coordenada(5, 5)));
        assertFalse(oceano.yaFueDisparada(new Coordenada(5, 4)));
        assertEquals(2, oceano.getCantidadPartesSanas());
        assertTrue(oceano.getTamanosBarcosHundidos().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"PORTAAVIONES,HORIZONTAL", "PORTAAVIONES,VERTICAL", "ACORAZADO,HORIZONTAL",
            "ACORAZADO,VERTICAL", "CRUCERO,HORIZONTAL", "CRUCERO,VERTICAL",
            "SUBMARINO,HORIZONTAL", "SUBMARINO,VERTICAL", "DESTRUCTOR,HORIZONTAL", "DESTRUCTOR,VERTICAL"})
    void soloLaUltimaParteHundeCadaTipoEnAmbasOrientaciones(TipoBarco tipo, Orientacion orientacion)
            throws Exception {
        Oceano oceano = new Oceano(6, 6, new ConfiguracionFlota(Map.of(tipo, 1)));
        Barco barco = oceano.colocarSiguienteBarco(new Coordenada(0, 0), orientacion);
        assertFalse(oceano.estaHundida());
        // Disparar desde la popa comprueba que hundir no depende del orden de los segmentos.
        for (int segmento = tipo.getTamano() - 1; segmento >= 0; segmento--) {
            Coordenada destino = orientacion == HORIZONTAL
                    ? new Coordenada(0, segmento) : new Coordenada(segmento, 0);
            Disparo disparo = oceano.recibirDisparo(destino);
            assertEquals(segmento == 0 ? HUNDIDO : IMPACTO, disparo.getResultado());
            assertEquals(segmento == 0 ? tipo.getTamano() : 0, disparo.getTamanoBarcoHundido());
            assertEquals(segmento, barco.getCantidadPartesSanas());
            assertEquals(segmento, oceano.getCantidadPartesSanas());
            assertEquals(segmento == 0, barco.estaHundido());
            assertEquals(segmento == 0, oceano.estaHundida());
        }
        assertEquals(tipo.getTamano(), oceano.getCantidadPartes());
        assertEquals(List.of(tipo.getTamano()), oceano.getTamanosBarcosHundidos());
    }

    @Test
    void impactosParcialesNoRevelanTamanoNiCambianAlHundirDespues() throws Exception {
        Oceano oceano = preparado(Map.of(CRUCERO, 1));
        Disparo primero = oceano.recibirDisparo(new Coordenada(0, 1));
        List<Disparo> historialAnterior = oceano.getDisparos();
        oceano.recibirDisparo(new Coordenada(0, 2));
        oceano.recibirDisparo(new Coordenada(0, 0));
        assertEquals(IMPACTO, primero.getResultado());
        assertEquals(0, primero.getTamanoBarcoHundido());
        assertEquals(1, historialAnterior.size());
        assertEquals(3, oceano.getDisparos().size());
    }

    @Test
    void bajasSiguenOrdenDeHundimientoYConservanBarcosDelMismoTamano() throws Exception {
        Oceano oceano = preparado(Map.of(CRUCERO, 1, SUBMARINO, 2));
        oceano.recibirDisparo(new Coordenada(0, 0));
        oceano.recibirDisparo(new Coordenada(2, 0));
        oceano.recibirDisparo(new Coordenada(4, 1));
        oceano.recibirDisparo(new Coordenada(4, 0));
        assertEquals(List.of(2), oceano.getTamanosBarcosHundidos());
        List<Integer> bajasAnteriores = oceano.getTamanosBarcosHundidos();
        oceano.recibirDisparo(new Coordenada(0, 1));
        oceano.recibirDisparo(new Coordenada(0, 2));
        assertEquals(List.of(2, 3), oceano.getTamanosBarcosHundidos());
        assertFalse(oceano.estaHundida());
        oceano.recibirDisparo(new Coordenada(2, 1));
        assertEquals(List.of(2, 3, 2), oceano.getTamanosBarcosHundidos());
        assertEquals(List.of(2), bajasAnteriores);
        assertTrue(oceano.estaHundida());
        assertEquals(0, oceano.getCantidadPartesSanas());
        assertEquals(7, oceano.getCantidadPartes());
    }

    @ParameterizedTest
    @CsvSource({"5,5", "0,0", "2,0"})
    void rechazaRepeticionDeAguaImpactoOHundimientoSinModificarNada(int fila, int columna)
            throws Exception {
        Oceano oceano = preparado(Map.of(CRUCERO, 1, DESTRUCTOR, 1));
        oceano.recibirDisparo(new Coordenada(fila, columna));
        BatallaException error = rechazaSinCambios(oceano,
                () -> oceano.recibirDisparo(new Coordenada(fila, columna)));
        assertEquals("Ya se disparó a esa celda", error.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "0,-1", "6,0", "0,10", "2147483647,0", "0,-2147483648"})
    void rechazaFueraDeRectanguloSinRegistrarNiDanar(int fila, int columna) throws Exception {
        Oceano oceano = new Oceano(10, 6, new ConfiguracionFlota(Map.of(SUBMARINO, 1)));
        oceano.colocarSiguienteBarco(new Coordenada(0, 0), HORIZONTAL);
        rechazaSinCambios(oceano, () -> oceano.recibirDisparo(new Coordenada(fila, columna)));
        assertEquals(IMPACTO, oceano.recibirDisparo(new Coordenada(0, 0)).getResultado());
    }

    @Test
    void rechazaCoordenadaNulaComoErrorDeDominio() throws Exception {
        Oceano oceano = preparado(Map.of(DESTRUCTOR, 1));
        rechazaSinCambios(oceano, () -> oceano.recibirDisparo(null));
    }

    @Test
    void exigeDespliegueCompletoYLaIntegridadIncluyeBarcosPendientes() throws Exception {
        Oceano oceano = new Oceano(6, 6, new ConfiguracionFlota(Map.of(CRUCERO, 1, SUBMARINO, 1)));
        assertEquals(5, oceano.getCantidadPartesSanas());
        assertFalse(oceano.estaHundida());
        rechazaSinCambios(oceano, () -> oceano.recibirDisparo(new Coordenada(0, 0)));
        oceano.colocarSiguienteBarco(new Coordenada(0, 0), HORIZONTAL);
        assertEquals(5, oceano.getCantidadPartesSanas());
        BatallaException error = rechazaSinCambios(oceano,
                () -> oceano.recibirDisparo(new Coordenada(0, 0)));
        assertEquals("Debe posicionar todos sus barcos primero", error.getMessage());
        oceano.colocarSiguienteBarco(new Coordenada(2, 0), HORIZONTAL);
        oceano.recibirDisparo(new Coordenada(0, 0));
        assertEquals(4, oceano.getCantidadPartesSanas());
    }

    @Test
    void flotaHundidaNoRecibeMasDisparosNiAgregaBajas() throws Exception {
        Oceano oceano = preparado(Map.of(DESTRUCTOR, 1));
        oceano.recibirDisparo(new Coordenada(0, 0));
        rechazaSinCambios(oceano, () -> oceano.recibirDisparo(new Coordenada(0, 0)));
        rechazaSinCambios(oceano, () -> oceano.recibirDisparo(new Coordenada(5, 5)));
        assertEquals(List.of(1), oceano.getTamanosBarcosHundidos());
    }

    @Test
    void consultasNoPermitenBorrarDanioDisparosOBajas() throws Exception {
        Oceano oceano = preparado(Map.of(CRUCERO, 1, DESTRUCTOR, 1));
        Barco crucero = oceano.getBarcos().getFirst();
        oceano.recibirDisparo(new Coordenada(0, 0));
        oceano.recibirDisparo(new Coordenada(2, 0));
        assertThrows(UnsupportedOperationException.class, () -> crucero.getImpactos().clear());
        assertThrows(UnsupportedOperationException.class, () -> oceano.getDisparos().clear());
        assertThrows(UnsupportedOperationException.class, () -> oceano.getTamanosBarcosHundidos().clear());
        assertEquals(2, oceano.getCantidadPartesSanas());
        assertEquals(2, oceano.getDisparos().size());
        assertEquals(List.of(1), oceano.getTamanosBarcosHundidos());
    }

    @Test
    void oceanoDaniadoNoAfectaOtroConLaMismaConfiguracion() throws Exception {
        ConfiguracionFlota flota = new ConfiguracionFlota(Map.of(DESTRUCTOR, 1));
        Oceano primero = new Oceano(6, 6, flota);
        Oceano segundo = new Oceano(6, 6, flota);
        primero.colocarSiguienteBarco(new Coordenada(0, 0), HORIZONTAL);
        segundo.colocarSiguienteBarco(new Coordenada(0, 0), HORIZONTAL);
        primero.recibirDisparo(new Coordenada(0, 0));
        assertTrue(primero.estaHundida());
        assertFalse(segundo.estaHundida());
        assertEquals(1, segundo.getCantidadPartesSanas());
        assertTrue(segundo.getDisparos().isEmpty());
        assertTrue(segundo.getTamanosBarcosHundidos().isEmpty());
    }

    @TestFactory
    List<DynamicTest> recorreCadaCeldaDeLos49RectangulosSinConfundirFilasYColumnas() {
        List<DynamicTest> pruebas = new ArrayList<>();
        for (int ancho = 6; ancho <= 12; ancho++) {
            for (int alto = 6; alto <= 12; alto++) {
                int columnas = ancho;
                int filas = alto;
                pruebas.add(DynamicTest.dynamicTest(columnas + "x" + filas, () -> {
                    Oceano oceano = new Oceano(columnas, filas, new ConfiguracionFlota(Map.of(DESTRUCTOR, 1)));
                    oceano.colocarSiguienteBarco(new Coordenada(filas - 1, columnas - 1), HORIZONTAL);
                    for (int fila = 0; fila < filas; fila++) {
                        for (int columna = 0; columna < columnas; columna++) {
                            Disparo disparo = oceano.recibirDisparo(new Coordenada(fila, columna));
                            boolean ultima = fila == filas - 1 && columna == columnas - 1;
                            assertEquals(ultima ? HUNDIDO : AGUA, disparo.getResultado());
                            assertEquals(ultima ? 0 : 1, oceano.getCantidadPartesSanas());
                        }
                    }
                    assertEquals(columnas * filas, oceano.getDisparos().size());
                    assertTrue(oceano.estaHundida());
                    assertEquals(List.of(1), oceano.getTamanosBarcosHundidos());
                }));
            }
        }
        return pruebas;
    }

    private Oceano preparado(Map<TipoBarco, Integer> cantidades) throws BatallaException {
        Oceano oceano = new Oceano(10, 10, new ConfiguracionFlota(cantidades));
        int fila = 0;
        while (!oceano.estaPreparado()) {
            oceano.colocarSiguienteBarco(new Coordenada(fila, 0), HORIZONTAL);
            fila += 2;
        }
        return oceano;
    }

    private BatallaException rechazaSinCambios(Oceano oceano, Executable accion) {
        List<Disparo> disparos = oceano.getDisparos();
        List<Integer> bajas = oceano.getTamanosBarcosHundidos();
        List<TipoBarco> pendientes = oceano.getBarcosPendientes();
        List<List<Coordenada>> impactos = new ArrayList<>();
        for (Barco barco : oceano.getBarcos()) {
            impactos.add(barco.getImpactos());
        }
        int partesSanas = oceano.getCantidadPartesSanas();
        BatallaException error = assertThrows(BatallaException.class, accion);
        assertEquals(disparos, oceano.getDisparos());
        assertEquals(bajas, oceano.getTamanosBarcosHundidos());
        assertEquals(pendientes, oceano.getBarcosPendientes());
        assertEquals(partesSanas, oceano.getCantidadPartesSanas());
        for (int indice = 0; indice < impactos.size(); indice++) {
            assertEquals(impactos.get(indice), oceano.getBarcos().get(indice).getImpactos());
        }
        return error;
    }
}
