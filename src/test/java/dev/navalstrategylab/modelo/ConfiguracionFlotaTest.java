package dev.navalstrategylab.modelo;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import dev.navalstrategylab.excepciones.BatallaException;
import static org.junit.jupiter.api.Assertions.*;
import static dev.navalstrategylab.modelo.TipoBarco.*;

class ConfiguracionFlotaTest {
    @ParameterizedTest
    @CsvSource({"PORTAAVIONES,Portaaviones,5", "ACORAZADO,Acorazado,4", "CRUCERO,Crucero,3",
            "SUBMARINO,Submarino,2", "DESTRUCTOR,Destructor,1"})
    void catalogoCoincideConLaLetra(TipoBarco tipo, String nombre, int tamano) {
        assertEquals(nombre, tipo.getNombre());
        assertEquals(tamano, tipo.getTamano());
        assertEquals(5, TipoBarco.values().length);
    }

    @ParameterizedTest
    @EnumSource(TipoBarco.class)
    void admiteDeUnaASeisUnidadesYRechazaCantidadesFueraDeRango(TipoBarco tipo) throws Exception {
        for (int cantidad = 1; cantidad <= 6; cantidad++) {
            ConfiguracionFlota flota = new ConfiguracionFlota(Map.of(tipo, cantidad));
            assertEquals(cantidad * tipo.getTamano(), flota.getCantidadCeldas());
        }
        for (int cantidad : new int[]{-1, 7, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            BatallaException error = assertThrows(BatallaException.class,
                    () -> new ConfiguracionFlota(Map.of(tipo, cantidad)));
            assertTrue(error.getMessage().contains(tipo.getNombre()));
        }
    }

    @Test
    void rechazaFlotaVaciaInclusoConCerosExplicitos() {
        assertThrows(BatallaException.class, () -> new ConfiguracionFlota(Map.of()));
        assertThrows(BatallaException.class,
                () -> new ConfiguracionFlota(Map.of(PORTAAVIONES, 0, DESTRUCTOR, 0)));
    }

    @Test
    void rechazaEntradasNulasConExcepcionDelDominio() {
        assertThrows(BatallaException.class, () -> new ConfiguracionFlota(null));
        Map<TipoBarco, Integer> cantidades = new HashMap<>();
        cantidades.put(null, 1);
        assertThrows(BatallaException.class, () -> new ConfiguracionFlota(cantidades));
        cantidades.clear();
        cantidades.put(DESTRUCTOR, null);
        assertThrows(BatallaException.class, () -> new ConfiguracionFlota(cantidades));
    }

    @ParameterizedTest
    @CsvSource({"6,6,8,true", "6,6,9,false", "6,6,10,false",
            "10,10,24,true", "10,10,25,false", "10,10,26,false",
            "6,12,17,true", "6,12,18,false", "12,6,17,true", "12,6,18,false",
            "12,12,35,true", "12,12,36,false"})
    void densidadEsEstrictamenteMenorAUnCuarto(int ancho, int alto, int celdas, boolean valido)
            throws Exception {
        // Se construye una composición válida en cantidades, con el área indicada.
        Map<TipoBarco, Integer> cantidades = new EnumMap<>(TipoBarco.class);
        int restantes = celdas;
        for (TipoBarco tipo : TipoBarco.values()) {
            int cantidad = Math.min(6, restantes / tipo.getTamano());
            cantidades.put(tipo, cantidad);
            restantes -= cantidad * tipo.getTamano();
        }
        assertEquals(0, restantes);
        ConfiguracionFlota flota = new ConfiguracionFlota(cantidades);
        if (valido) {
            assertDoesNotThrow(() -> new Oceano(ancho, alto, flota));
        } else {
            BatallaException error = assertThrows(BatallaException.class,
                    () -> new Oceano(ancho, alto, flota));
            assertTrue(error.getMessage().contains("25%"));
        }
    }

    @Test
    void conservaComposicionYEntregaUnidadesOrdenadasSinCompartirLista() throws Exception {
        Map<TipoBarco, Integer> entrada = new HashMap<>();
        entrada.put(DESTRUCTOR, 2);
        entrada.put(PORTAAVIONES, 1);
        entrada.put(SUBMARINO, 1);
        ConfiguracionFlota flota = new ConfiguracionFlota(entrada);
        entrada.clear();
        List<TipoBarco> esperada = List.of(PORTAAVIONES, SUBMARINO, DESTRUCTOR, DESTRUCTOR);
        assertEquals(esperada, flota.crearListaBarcos());
        flota.crearListaBarcos().clear();
        assertEquals(esperada, flota.crearListaBarcos());
        assertEquals(0, flota.getCantidad(CRUCERO));
        assertEquals(9, flota.getCantidadCeldas());
    }
}
