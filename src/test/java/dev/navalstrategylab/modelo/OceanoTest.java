package dev.navalstrategylab.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.function.Executable;
import dev.navalstrategylab.excepciones.BatallaException;
import static org.junit.jupiter.api.Assertions.*;
import static dev.navalstrategylab.modelo.TipoBarco.*;
import static dev.navalstrategylab.modelo.Orientacion.*;

class OceanoTest {
    @TestFactory
    List<DynamicTest> admiteTodasLasDimensionesValidasSinInvertirEjes() {
        List<DynamicTest> pruebas = new ArrayList<>();
        for (int ancho = 6; ancho <= 12; ancho++) {
            for (int alto = 6; alto <= 12; alto++) {
                int columnas = ancho;
                int filas = alto;
                pruebas.add(DynamicTest.dynamicTest(columnas + "x" + filas, () -> {
                    Oceano oceano = oceano(columnas, filas, Map.of(DESTRUCTOR, 1));
                    assertEquals(columnas, oceano.getAncho());
                    assertEquals(filas, oceano.getAlto());
                    assertTrue(oceano.contiene(new Coordenada(0, 0)));
                    assertTrue(oceano.contiene(new Coordenada(filas - 1, columnas - 1)));
                    assertFalse(oceano.contiene(new Coordenada(filas, 0)));
                    assertFalse(oceano.contiene(new Coordenada(0, columnas)));
                    oceano.colocarSiguienteBarco(new Coordenada(filas - 1, columnas - 1), HORIZONTAL);
                    assertTrue(oceano.estaPreparado());
                }));
            }
        }
        return pruebas;
    }

    @ParameterizedTest
    @CsvSource({"5,6,horizontal", "13,6,horizontal", "6,5,vertical", "6,13,vertical",
            "-1,6,horizontal", "6,2147483647,vertical"})
    void rechazaCadaDimensionInvalida(int ancho, int alto, String eje) {
        BatallaException error = assertThrows(BatallaException.class,
                () -> oceano(ancho, alto, Map.of(DESTRUCTOR, 1)));
        assertTrue(error.getMessage().contains(eje));
    }

    @Test
    void exigeFlota() {
        assertThrows(BatallaException.class, () -> new Oceano(6, 6, null));
    }

    @ParameterizedTest
    @CsvSource({"HORIZONTAL,2,3,2,4,2,5", "VERTICAL,3,2,4,2,5,2"})
    void colocaSegmentosConsecutivosDesdeLaProa(Orientacion orientacion, int f1, int c1,
            int f2, int c2, int f3, int c3) throws Exception {
        Oceano oceano = oceano(6, 6, Map.of(CRUCERO, 1));
        Barco barco = oceano.colocarSiguienteBarco(new Coordenada(f1, c1), orientacion);
        assertEquals(List.of(new Coordenada(f1, c1), new Coordenada(f2, c2),
                new Coordenada(f3, c3)), barco.getCeldas());
        assertTrue(oceano.estaPreparado());
    }

    @ParameterizedTest
    @CsvSource({"HORIZONTAL,5,3", "VERTICAL,3,5"})
    void admiteUltimoSegmentoExactamenteEnElBorde(Orientacion orientacion, int fila, int columna)
            throws Exception {
        Oceano oceano = oceano(6, 6, Map.of(CRUCERO, 1));
        Barco barco = oceano.colocarSiguienteBarco(new Coordenada(fila, columna), orientacion);
        assertEquals(new Coordenada(5, 5), barco.getCeldas().getLast());
    }

    @ParameterizedTest
    @CsvSource({"HORIZONTAL,5,4", "VERTICAL,4,5", "HORIZONTAL,-1,0", "VERTICAL,0,-1",
            "HORIZONTAL,6,0", "VERTICAL,0,6", "HORIZONTAL,0,2147483647"})
    void rechazaProaOExtremoFueraSinConsumirBarco(Orientacion orientacion, int fila, int columna)
            throws Exception {
        Oceano oceano = oceano(6, 6, Map.of(CRUCERO, 1));
        rechazaSinCambios(oceano,
                () -> oceano.colocarSiguienteBarco(new Coordenada(fila, columna), orientacion));
    }

    @Test
    void rechazaProaYOrientacionAusentesSinCambios() throws Exception {
        Oceano oceano = oceano(6, 6, Map.of(DESTRUCTOR, 1));
        rechazaSinCambios(oceano, () -> oceano.colocarSiguienteBarco(null, HORIZONTAL));
        rechazaSinCambios(oceano, () -> oceano.colocarSiguienteBarco(new Coordenada(0, 0), null));
    }

    @ParameterizedTest
    @CsvSource({"0,0,HORIZONTAL", "0,1,HORIZONTAL", "0,2,VERTICAL"})
    void rechazaSuperposicionTotalParcialYEnCruce(int fila, int columna, Orientacion orientacion)
            throws Exception {
        Oceano oceano = oceano(8, 8, Map.of(CRUCERO, 2));
        oceano.colocarSiguienteBarco(new Coordenada(0, 0), HORIZONTAL);
        rechazaSinCambios(oceano,
                () -> oceano.colocarSiguienteBarco(new Coordenada(fila, columna), orientacion));
    }

    @ParameterizedTest
    @CsvSource({"2,2", "2,3", "2,4", "3,2", "3,4", "4,2", "4,3", "4,4"})
    void rechazaContactoEnLasOchoDirecciones(int fila, int columna) throws Exception {
        Oceano oceano = oceano(6, 6, Map.of(DESTRUCTOR, 2));
        oceano.colocarSiguienteBarco(new Coordenada(3, 3), HORIZONTAL);
        rechazaSinCambios(oceano,
                () -> oceano.colocarSiguienteBarco(new Coordenada(fila, columna), HORIZONTAL));
    }

    @Test
    void validaContactoDelExtremoAunqueLaProaEsteLejos() throws Exception {
        Oceano oceano = oceano(8, 8, Map.of(CRUCERO, 2));
        oceano.colocarSiguienteBarco(new Coordenada(0, 0), HORIZONTAL);
        rechazaSinCambios(oceano, () -> oceano.colocarSiguienteBarco(new Coordenada(1, 3), VERTICAL));
        // El segundo extremo llega a la diagonal del primer barco.
        Oceano inverso = oceano(8, 8, Map.of(CRUCERO, 2));
        inverso.colocarSiguienteBarco(new Coordenada(3, 3), HORIZONTAL);
        rechazaSinCambios(inverso, () -> inverso.colocarSiguienteBarco(new Coordenada(0, 2), VERTICAL));
    }

    @ParameterizedTest
    @CsvSource({"3,1", "3,5", "1,3", "5,3", "1,1", "5,5"})
    void permiteUnaCeldaDeAguaDeSeparacion(int fila, int columna) throws Exception {
        Oceano oceano = oceano(6, 6, Map.of(DESTRUCTOR, 2));
        oceano.colocarSiguienteBarco(new Coordenada(3, 3), HORIZONTAL);
        oceano.colocarSiguienteBarco(new Coordenada(fila, columna), VERTICAL);
        assertTrue(oceano.estaPreparado());
        assertEquals(2, oceano.getBarcos().size());
    }

    @Test
    void colocaSoloElPrimeroPendienteYConservaDuplicados() throws Exception {
        Oceano oceano = oceano(10, 10, Map.of(DESTRUCTOR, 2, CRUCERO, 1));
        assertEquals(List.of(CRUCERO, DESTRUCTOR, DESTRUCTOR), oceano.getBarcosPendientes());
        assertEquals(CRUCERO, oceano.colocarSiguienteBarco(new Coordenada(0, 0), HORIZONTAL).getTipo());
        assertEquals(List.of(DESTRUCTOR, DESTRUCTOR), oceano.getBarcosPendientes());
        assertFalse(oceano.estaPreparado());
        oceano.colocarSiguienteBarco(new Coordenada(2, 0), HORIZONTAL);
        oceano.colocarSiguienteBarco(new Coordenada(4, 0), HORIZONTAL);
        assertTrue(oceano.estaPreparado());
        rechazaSinCambios(oceano, () -> oceano.colocarSiguienteBarco(new Coordenada(6, 0), HORIZONTAL));
    }

    @Test
    void dosOceanosCompartenComposicionPeroNoColocaciones() throws Exception {
        ConfiguracionFlota flota = new ConfiguracionFlota(Map.of(SUBMARINO, 1));
        Oceano propio = new Oceano(6, 8, flota);
        Oceano enemigo = new Oceano(6, 8, flota);
        propio.colocarSiguienteBarco(new Coordenada(0, 0), HORIZONTAL);
        assertFalse(enemigo.estaPreparado());
        assertTrue(enemigo.getBarcos().isEmpty());
        enemigo.colocarSiguienteBarco(new Coordenada(6, 5), VERTICAL);
        assertEquals(List.of(new Coordenada(0, 0), new Coordenada(0, 1)),
                propio.getBarcos().getFirst().getCeldas());
    }

    @Test
    void consultasNoPermitenSaltarValidacionesNiCambianRetrospectivamente() throws Exception {
        Oceano oceano = oceano(6, 6, Map.of(DESTRUCTOR, 1));
        List<TipoBarco> pendientes = oceano.getBarcosPendientes();
        List<Barco> colocados = oceano.getBarcos();
        assertThrows(UnsupportedOperationException.class, pendientes::clear);
        Barco barco = oceano.colocarSiguienteBarco(new Coordenada(0, 0), HORIZONTAL);
        assertThrows(UnsupportedOperationException.class, () -> oceano.getBarcos().clear());
        assertThrows(UnsupportedOperationException.class, () -> barco.getCeldas().clear());
        assertEquals(List.of(DESTRUCTOR), pendientes);
        assertTrue(colocados.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Orientacion.class)
    void puedeReintentarCorrectamenteDespuesDeError(Orientacion orientacion) throws Exception {
        Oceano oceano = oceano(6, 6, Map.of(CRUCERO, 1));
        rechazaSinCambios(oceano, () -> oceano.colocarSiguienteBarco(new Coordenada(5, 5), orientacion));
        oceano.colocarSiguienteBarco(new Coordenada(0, 0), orientacion);
        assertTrue(oceano.estaPreparado());
    }

    private Oceano oceano(int ancho, int alto, Map<TipoBarco, Integer> cantidades) throws BatallaException {
        return new Oceano(ancho, alto, new ConfiguracionFlota(cantidades));
    }

    private void rechazaSinCambios(Oceano oceano, Executable accion) {
        List<Barco> antes = oceano.getBarcos();
        List<TipoBarco> pendientesAntes = oceano.getBarcosPendientes();
        assertThrows(BatallaException.class, accion);
        assertEquals(antes, oceano.getBarcos());
        assertEquals(pendientesAntes, oceano.getBarcosPendientes());
    }
}
