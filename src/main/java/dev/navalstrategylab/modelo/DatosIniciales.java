package dev.navalstrategylab.modelo;

import java.util.Map;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.tacticas.*;

public final class DatosIniciales {
    private DatosIniciales() { }

    public static SistemaBatallas crearSistema() throws BatallaException {
        SistemaBatallas sistema = new SistemaBatallas();
        Map<TipoBarco, Integer> cantidades = Map.of(TipoBarco.SUBMARINO, 1, TipoBarco.DESTRUCTOR, 1);
        ConfiguracionFlota flota = new ConfiguracionFlota(cantidades);
        sistema.crear("Atlantico", 6, 6, cantidades, "Espiral");
        Batalla combate = new Batalla("Pacifico", propio(flota), enemigo(flota), new CazaYAtaque(new java.util.Random(17)));
        combate.iniciar();
        combate.disparar(1, new Coordenada(5, 5));
        sistema.incorporar(combate);
        Batalla ganada = new Batalla("Indico", propio(flota), enemigo(flota), new Secuencial());
        ganada.iniciar();
        ganada.disparar(1, new Coordenada(4, 0));
        ganada.disparar(2, new Coordenada(4, 1));
        ganada.disparar(3, new Coordenada(2, 4));
        sistema.incorporar(ganada);
        Batalla perdida = new Batalla("Artico", propio(flota), enemigo(flota), new Secuencial());
        perdida.iniciar();
        for (int fila = 0; fila < 6 && perdida.consultar().getSituacion() == SituacionBatalla.EN_COMBATE; fila++) {
            for (int columna = 0; columna < 6 && perdida.consultar().getSituacion() == SituacionBatalla.EN_COMBATE; columna++) {
                // El guion de muestra elige agua; no se utiliza como tactica de la maquina.
                if ((fila == 4 && columna < 2) || (fila == 2 && columna == 4)) continue;
                perdida.disparar(perdida.consultar().getRonda(), new Coordenada(fila, columna));
            }
        }
        sistema.incorporar(perdida);
        return sistema;
    }

    private static Oceano propio(ConfiguracionFlota flota) throws BatallaException {
        Oceano oceano = new Oceano(6, 6, flota);
        oceano.colocarSiguienteBarco(new Coordenada(0, 0), Orientacion.HORIZONTAL);
        oceano.colocarSiguienteBarco(new Coordenada(2, 0), Orientacion.HORIZONTAL);
        return oceano;
    }
    private static Oceano enemigo(ConfiguracionFlota flota) throws BatallaException {
        Oceano oceano = new Oceano(6, 6, flota);
        oceano.colocarSiguienteBarco(new Coordenada(4, 0), Orientacion.HORIZONTAL);
        oceano.colocarSiguienteBarco(new Coordenada(2, 4), Orientacion.HORIZONTAL);
        return oceano;
    }
}
