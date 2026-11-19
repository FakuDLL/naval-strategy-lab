package dev.navalstrategylab.modelo;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.tacticas.*;
import static org.junit.jupiter.api.Assertions.*;

class SistemaBatallasTest {
    private final Map<TipoBarco, Integer> flota = Map.of(TipoBarco.SUBMARINO, 1, TipoBarco.DESTRUCTOR, 1);

    @Test void precargaTieneCuatroEstadosValidosYDosTiposPorBatalla() throws Exception {
        Fachada fachada = new Fachada(DatosIniciales.crearSistema());
        ResumenCampana sala = fachada.consultarSala();
        assertEquals(4, sala.getTotal());
        assertEquals(1, sala.getGanadas());
        assertEquals(1, sala.getPerdidas());
        assertEquals(25, sala.getPorcentajeExito());
        assertEquals(Set.of(SituacionBatalla.values()), new HashSet<>(sala.getBatallas().stream().map(InstantaneaBatalla::getSituacion).toList()));
        InstantaneaBatalla preparacion = fachada.consultarBatalla("Atlantico");
        assertTrue(preparacion.getPropio().getCeldasBarcos().isEmpty());
        assertEquals(List.of(TipoBarco.SUBMARINO, TipoBarco.DESTRUCTOR), preparacion.getPropio().getPendientes());
        assertTrue(preparacion.getEnemigo().getCeldasBarcos().isEmpty());
        assertEquals(3, fachada.consultarBatalla("Indico").getEnemigo().getDisparos().size());
        assertEquals(0, fachada.consultarBatalla("Indico").getEnemigo().getPartesSanas());
        assertEquals(0, fachada.consultarBatalla("Artico").getPropio().getPartesSanas());
        assertEquals(3, fachada.consultarBatalla("Artico").getEnemigo().getCeldasBarcos().size());
        assertThrows(BatallaException.class, () -> fachada.disparar("Indico", 3, new Coordenada(5, 5)));
        assertThrows(BatallaException.class, () -> fachada.iniciarBatalla("Artico"));
    }
    @Test void creacionYReentradaConservanPreparacionYVersion() throws Exception {
        Fachada f = new Fachada(new SistemaBatallas());
        InstantaneaBatalla inicial = f.crearBatalla(" Mar del Sur ", 7, 6, flota, "Espiral");
        assertEquals("Mar del Sur", inicial.getNombre());
        f.colocarBarco("mar del sur", 0, new Coordenada(0, 0), Orientacion.HORIZONTAL);
        assertEquals(1, f.consultarBatalla("Mar del Sur").getVersion());
        assertEquals(2, f.consultarSala().getBatallas().getFirst().getPropio().getCeldasBarcos().size());
        assertTrue(inicial.getPropio().getCeldasBarcos().isEmpty());
        assertThrows(BatallaException.class, () -> f.iniciarBatalla("Mar del Sur"));
        f.colocarBarco("Mar del Sur", 1, new Coordenada(3, 0), Orientacion.VERTICAL);
        assertEquals(SituacionBatalla.EN_COMBATE, f.iniciarBatalla("Mar del Sur").getSituacion());
        assertEquals(1, f.disparar("Mar del Sur", 1, new Coordenada(0, 0)).getPropio().getDisparos().size());
    }
    @Test void despliegaFlotaEnemigaAlLibrarLaBatalla() throws Exception {
        class AzarDeDespliegue extends Random {
            int llamadas;

            @Override public int nextInt(int limite) {
                llamadas++;
                return super.nextInt(limite);
            }
        }
        AzarDeDespliegue azar = new AzarDeDespliegue();
        SistemaBatallas sistema = new SistemaBatallas(new CatalogoTacticas(), new DespliegueAutomatico(azar), new Random());
        sistema.crear("Mar", 6, 6, flota, "Secuencial");
        assertEquals(0, azar.llamadas);
        assertThrows(BatallaException.class, () -> sistema.iniciar("Mar"));
        assertEquals(0, azar.llamadas);

        sistema.colocar("Mar", 0, new Coordenada(4, 0), Orientacion.HORIZONTAL);
        sistema.colocar("Mar", 1, new Coordenada(2, 0), Orientacion.HORIZONTAL);
        var iniciada = sistema.iniciar("Mar");
        assertTrue(azar.llamadas > 0);
        assertEquals(SituacionBatalla.EN_COMBATE, iniciada.getSituacion());
        assertEquals(3, iniciada.getEnemigo().getPartesSanas());
        assertTrue(iniciada.getEnemigo().getCeldasBarcos().isEmpty());
        assertEquals(1, sistema.disparar("Mar", 1, new Coordenada(0, 0)).getEnemigo().getDisparos().size());
    }
    @Test void invalidosNoAgreganBatallas() throws Exception {
        SistemaBatallas s = new SistemaBatallas();
        assertEquals(0, s.consultarSala().getPorcentajeExito());
        assertThrows(BatallaException.class, () -> s.crear("Mar", 5, 6, flota, "Secuencial"));
        assertThrows(BatallaException.class, () -> s.crear("Mar", 6, 6, Map.of(), "Secuencial"));
        assertThrows(BatallaException.class, () -> s.crear("Mar", 6, 6, flota, "Inexistente"));
        assertEquals(0, s.consultarSala().getTotal());
        assertThrows(BatallaException.class, () -> s.consultar(null));
        assertThrows(BatallaException.class, () -> s.consultar("Inexistente"));
        s.crear("Mar", 6, 6, flota, "Secuencial");
        assertThrows(BatallaException.class, () -> s.crear(" MAR ", 6, 6, flota, "Espiral"));
        assertEquals(1, s.consultarSala().getTotal());
    }
    @Test void nombresAutomaticosSonUnicosYAlfabeticos() throws Exception {
        SistemaBatallas s = new SistemaBatallas();
        Set<String> nombres = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            String nombre = s.crear(i % 2 == 0 ? null : "   ", 6, 6, flota, "Secuencial").getNombre();
            assertTrue(nombre.matches("[A-Z]{6}"));
            assertTrue(nombres.add(nombre));
        }
        assertEquals(30, s.consultarSala().getTotal());
    }
    @Test void colisionesDeAzarTerminanSinAgregarDuplicados() throws Exception {
        Random constante = new Random() { @Override public int nextInt(int bound) { return 0; } };
        SistemaBatallas s = new SistemaBatallas(new CatalogoTacticas(), new DespliegueAutomatico(), constante);
        s.crear(null, 6, 6, flota, "Secuencial");
        assertThrows(BatallaException.class, () -> s.crear(null, 6, 6, flota, "Secuencial"));
        assertEquals(1, s.consultarSala().getTotal());
    }
    @Test void creacionConcurrenteNoDuplicaLugar() throws Exception {
        SistemaBatallas s = new SistemaBatallas();
        CountDownLatch comenzar = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> crear = () -> {
                comenzar.await();
                try { s.crear("Mar", 6, 6, flota, "Secuencial"); return true; }
                catch (BatallaException e) { return false; }
            };
            Future<Boolean> a = pool.submit(crear), b = pool.submit(crear);
            comenzar.countDown();
            assertNotEquals(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, s.consultarSala().getTotal());
    }
    @Test void catalogoEsExtensibleSinCambiarBatallaYNoComparteInstancias() throws Exception {
        CatalogoTacticas c = new CatalogoTacticas();
        assertEquals(List.of("Secuencial", "Espiral", "Caza y ataque"), c.listar());
        assertNotSame(c.crear("Caza y ataque"), c.crear("Caza y ataque"));
        c.registrar("Nueva", () -> new Tactica() {
            public String getNombre() { return "Nueva"; }
            public Coordenada elegirDisparo(InformacionDisparos i) throws BatallaException {
                return new Secuencial().elegirDisparo(i);
            }
        });
        SistemaBatallas s = new SistemaBatallas(c, new DespliegueAutomatico(), new Random(1));
        assertEquals(SituacionBatalla.EN_PREPARACION, s.crear("Mar", 6, 6, flota, "Nueva").getSituacion());
        assertThrows(IllegalArgumentException.class, () -> c.registrar("Nueva", Secuencial::new));
        assertThrows(UnsupportedOperationException.class, () -> c.listar().clear());
        c.registrar("Mal configurada", Secuencial::new);
        assertThrows(IllegalStateException.class, () -> c.crear("Mal configurada"));
        assertThrows(UnsupportedOperationException.class, () -> s.consultarSala().getBatallas().clear());
        assertThrows(UnsupportedOperationException.class, () -> s.listarTiposBarco().clear());
    }
}
