package dev.navalstrategylab.modelo;

import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.tacticas.*;
import static org.junit.jupiter.api.Assertions.*;

class BatallaTest {
    private Oceano oceano(int fila) throws Exception {
        Oceano o = new Oceano(6, 6, new ConfiguracionFlota(Map.of(TipoBarco.SUBMARINO, 1)));
        o.colocarSiguienteBarco(new Coordenada(fila, 0), Orientacion.HORIZONTAL);
        return o;
    }
    private Batalla batalla(Tactica tactica) throws Exception {
        return new Batalla("Atlantico", oceano(5), oceano(3), tactica);
    }
    private Tactica fija(Coordenada destino) {
        return new Tactica() {
            public String getNombre() { return "Prueba"; }
            public Coordenada elegirDisparo(InformacionDisparos i) { return destino; }
        };
    }
    @Test void preparacionYVersionProtegenLaColocacion() throws Exception {
        Oceano propio = new Oceano(6, 6, new ConfiguracionFlota(Map.of(TipoBarco.SUBMARINO, 1)));
        Batalla b = new Batalla("Mar", propio, oceano(3), new Secuencial());
        assertThrows(BatallaException.class, b::iniciar);
        assertThrows(BatallaException.class, () -> b.disparar(1, new Coordenada(0, 0)));
        assertEquals(0, b.consultar().getVersion());
        b.colocar(0, new Coordenada(5, 0), Orientacion.HORIZONTAL);
        assertThrows(BatallaException.class, () -> b.colocar(0, new Coordenada(0, 0), Orientacion.HORIZONTAL));
        b.iniciar();
        assertThrows(BatallaException.class, b::iniciar);
        assertThrows(BatallaException.class, () -> b.colocar(2, new Coordenada(0, 0), Orientacion.HORIZONTAL));
        assertEquals(SituacionBatalla.EN_COMBATE, b.consultar().getSituacion());
    }
    @Test void rondaCompletaEInstantaneasIndependientes() throws Exception {
        Batalla b = batalla(new Secuencial());
        InstantaneaBatalla anterior = b.iniciar();
        InstantaneaBatalla actual = b.disparar(1, new Coordenada(3, 0));
        assertEquals(2, actual.getRonda());
        assertEquals(1, actual.getEnemigo().getPartesSanas());
        assertEquals(1, actual.getPropio().getDisparos().size());
        assertTrue(actual.getEnemigo().getCeldasBarcos().isEmpty());
        assertTrue(anterior.getEnemigo().getDisparos().isEmpty());
        assertEquals(2, anterior.getEnemigo().getPartesSanas());
        assertThrows(UnsupportedOperationException.class, () -> actual.getPropio().getCeldasBarcos().clear());
        assertThrows(UnsupportedOperationException.class, () -> actual.getEnemigo().getDisparos().clear());
    }
    @Test void victoriaNoRespondeNiPermiteContinuar() throws Exception {
        Batalla b = batalla(fija(new Coordenada(0, 0)));
        b.iniciar();
        b.disparar(1, new Coordenada(3, 0));
        // Una segunda respuesta repetida fallaria: la victoria debe omitirla.
        InstantaneaBatalla fin = b.disparar(2, new Coordenada(3, 1));
        assertEquals(SituacionBatalla.GANADA, fin.getSituacion());
        assertEquals(1, fin.getPropio().getDisparos().size());
        assertEquals(2, fin.getRonda());
        assertThrows(BatallaException.class, () -> b.disparar(2, new Coordenada(2, 0)));
        assertThrows(BatallaException.class, b::iniciar);
    }
    @Test void derrotaRevelaEnemigoSoloAlFinal() throws Exception {
        Batalla b = new Batalla("Mar", oceano(0), oceano(3), new Secuencial());
        b.iniciar();
        assertTrue(b.disparar(1, new Coordenada(4, 0)).getEnemigo().getCeldasBarcos().isEmpty());
        InstantaneaBatalla fin = b.disparar(2, new Coordenada(4, 1));
        assertEquals(SituacionBatalla.PERDIDA, fin.getSituacion());
        assertEquals(2, fin.getEnemigo().getCeldasBarcos().size());
        assertEquals(0, fin.getPropio().getPartesSanas());
    }
    @Test void rechazosNoConsumenRondaNiRespuesta() throws Exception {
        Batalla b = batalla(new Secuencial());
        b.iniciar();
        assertThrows(BatallaException.class, () -> b.disparar(2, new Coordenada(0, 0)));
        assertThrows(BatallaException.class, () -> b.disparar(1, new Coordenada(-1, 0)));
        b.disparar(1, new Coordenada(0, 0));
        assertThrows(BatallaException.class, () -> b.disparar(1, new Coordenada(0, 1)));
        assertThrows(BatallaException.class, () -> b.disparar(2, new Coordenada(0, 0)));
        assertEquals(2, b.consultar().getVersion());
        assertEquals(1, b.consultar().getPropio().getDisparos().size());
    }
    @Test void respuestaInvalidaRevierteTambienImpactoHumano() throws Exception {
        Batalla b = batalla(fija(new Coordenada(8, 8)));
        b.iniciar();
        assertThrows(BatallaException.class, () -> b.disparar(1, new Coordenada(3, 0)));
        assertEquals(2, b.consultar().getEnemigo().getPartesSanas());
        assertTrue(b.consultar().getEnemigo().getDisparos().isEmpty());
        assertTrue(b.consultar().getPropio().getDisparos().isEmpty());
        assertEquals(1, b.consultar().getRonda());
        assertEquals(1, b.consultar().getVersion());
    }
    @Test void constructorAislaOceanoExterno() throws Exception {
        Oceano propio = oceano(0), enemigo = oceano(3);
        Batalla b = new Batalla("Mar", propio, enemigo, new Secuencial());
        propio.recibirDisparo(new Coordenada(0, 0));
        enemigo.recibirDisparo(new Coordenada(3, 0));
        assertEquals(2, b.consultar().getPropio().getPartesSanas());
        assertEquals(2, b.consultar().getEnemigo().getPartesSanas());
        assertThrows(BatallaException.class, () -> new Batalla("Otro", propio, enemigo, new Secuencial()));
    }
    @Test void dosSolicitudesSimultaneasSoloAplicanUnaRonda() throws Exception {
        Batalla b = batalla(new Secuencial()); b.iniciar();
        CountDownLatch salida = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.atomic.AtomicInteger columna = new java.util.concurrent.atomic.AtomicInteger();
            Callable<Boolean> accion = () -> {
                salida.await();
                try { b.disparar(1, new Coordenada(0, columna.getAndIncrement())); return true; }
                catch (BatallaException e) { return false; }
            };
            Future<Boolean> a = pool.submit(accion), c = pool.submit(accion);
            salida.countDown();
            assertNotEquals(a.get(3, TimeUnit.SECONDS), c.get(3, TimeUnit.SECONDS));
        }
        assertEquals(2, b.consultar().getRonda());
        assertEquals(1, b.consultar().getEnemigo().getDisparos().size());
        assertEquals(1, b.consultar().getPropio().getDisparos().size());
    }
    @Test void consultaNoObservaMediaRondaYOtraBatallaSigueDisponible() throws Exception {
        CountDownLatch entrando = new CountDownLatch(1), liberar = new CountDownLatch(1);
        Tactica lenta = new Tactica() {
            public String getNombre() { return "Controlada"; }
            public Coordenada elegirDisparo(InformacionDisparos i) throws BatallaException {
                entrando.countDown();
                try { if (!liberar.await(5, TimeUnit.SECONDS)) throw new BatallaException("Tiempo agotado"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new BatallaException("Interrumpida"); }
                return new Coordenada(0, 0);
            }
        };
        Batalla b = batalla(lenta), otra = batalla(new Secuencial()); b.iniciar(); otra.iniciar();
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<?> ronda = pool.submit(() -> b.disparar(1, new Coordenada(3, 0)));
            try {
                assertTrue(entrando.await(3, TimeUnit.SECONDS));
                Future<InstantaneaBatalla> consulta = pool.submit(b::consultar);
                assertThrows(TimeoutException.class, () -> consulta.get(100, TimeUnit.MILLISECONDS));
                assertEquals(2, otra.disparar(1, new Coordenada(0, 0)).getRonda());
                liberar.countDown();
                ronda.get(3, TimeUnit.SECONDS);
                InstantaneaBatalla vista = consulta.get(3, TimeUnit.SECONDS);
                assertEquals(1, vista.getPropio().getDisparos().size());
                assertEquals(1, vista.getEnemigo().getDisparos().size());
            } finally { liberar.countDown(); }
        }
    }
    @Test void configuracionesIncompatiblesYDatosInicialesInvalidosSeRechazan() throws Exception {
        Oceano propio = oceano(0), enemigo = oceano(3);
        assertThrows(BatallaException.class, () -> new Batalla(" ", propio, enemigo, new Secuencial()));
        assertThrows(BatallaException.class, () -> new Batalla("Mar", null, enemigo, new Secuencial()));
        assertThrows(BatallaException.class, () -> new Batalla("Mar", propio, enemigo, null));
        Oceano incompleto = new Oceano(6, 6, new ConfiguracionFlota(Map.of(TipoBarco.SUBMARINO, 1)));
        assertThrows(BatallaException.class, () -> new Batalla("Mar", propio, incompleto, new Secuencial()));
        Oceano distinto = new Oceano(7, 6, new ConfiguracionFlota(Map.of(TipoBarco.SUBMARINO, 1)));
        distinto.colocarSiguienteBarco(new Coordenada(0, 0), Orientacion.HORIZONTAL);
        assertThrows(BatallaException.class, () -> new Batalla("Mar", propio, distinto, new Secuencial()));
        Oceano otraFlota = new Oceano(6, 6, new ConfiguracionFlota(Map.of(TipoBarco.DESTRUCTOR, 2)));
        otraFlota.colocarSiguienteBarco(new Coordenada(0, 0), Orientacion.HORIZONTAL);
        otraFlota.colocarSiguienteBarco(new Coordenada(3, 0), Orientacion.HORIZONTAL);
        assertThrows(BatallaException.class, () -> new Batalla("Mar", propio, otraFlota, new Secuencial()));
    }
    @Test void falloInesperadoDeTacticaTampocoConfirmaMediaRonda() throws Exception {
        Tactica rota = new Tactica() {
            public String getNombre() { return "Prueba"; }
            public Coordenada elegirDisparo(InformacionDisparos i) { throw new IllegalStateException("Fallo simulado"); }
        };
        Batalla b = batalla(rota); b.iniciar();
        assertThrows(IllegalStateException.class, () -> b.disparar(1, new Coordenada(3, 0)));
        assertEquals(2, b.consultar().getEnemigo().getPartesSanas());
        assertEquals(1, b.consultar().getVersion());
        assertTrue(b.consultar().getEnemigo().getDisparos().isEmpty());
    }
}
