package dev.navalstrategylab.modelo;

import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.tacticas.InformacionDisparos;
import dev.navalstrategylab.modelo.tacticas.Tactica;

public final class Batalla {
    private final String nombre;
    private final Tactica tactica;
    private final DespliegueAutomatico despliegue;
    private Oceano propio;
    private Oceano enemigo;
    private EstadoBatalla estado = new EnPreparacion();
    private int ronda = 1;
    private long version;

    public Batalla(String nombre, Oceano propio, Oceano enemigo, Tactica tactica) throws BatallaException {
        this(nombre, propio, enemigo, tactica, null);
    }

    Batalla(String nombre, Oceano propio, Oceano enemigo, Tactica tactica,
            DespliegueAutomatico despliegue) throws BatallaException {
        if (nombre == null || nombre.isBlank()) { throw new BatallaException("Debe indicar un lugar de combate"); }
        if (propio == null || enemigo == null || tactica == null) {
            throw new BatallaException("Debe indicar océanos y táctica para la batalla");
        }
        if (!propio.tieneMismaConfiguracion(enemigo)) {
            throw new BatallaException("Los océanos deben tener iguales dimensiones y composición de flota");
        }
        if ((despliegue == null && !enemigo.estaPreparado())
                || !propio.getDisparos().isEmpty() || !enemigo.getDisparos().isEmpty()) {
            throw new BatallaException("La flota enemiga debe estar desplegada y los océanos sin disparos");
        }
        this.nombre = nombre;
        this.propio = propio.copiar();
        this.enemigo = enemigo.copiar();
        this.tactica = tactica;
        this.despliegue = despliegue;
    }

    public synchronized InstantaneaBatalla colocar(long versionEsperada, Coordenada proa,
            Orientacion orientacion) throws BatallaException {
        if (versionEsperada != version) {
            throw new BatallaException("La preparación cambió; vuelva a intentar con la situación actual");
        }
        estado.colocar(this, proa, orientacion);
        version++;
        return consultar();
    }

    public synchronized InstantaneaBatalla iniciar() throws BatallaException {
        estado = estado.iniciar(this);
        version++;
        return consultar();
    }

    public synchronized InstantaneaBatalla disparar(int rondaEsperada, Coordenada destino) throws BatallaException {
        estado = estado.disparar(this, rondaEsperada, destino);
        version++;
        return consultar();
    }

    public synchronized InstantaneaBatalla consultar() {
        return new InstantaneaBatalla(nombre, tactica.getNombre(), estado.getSituacion(), ronda,
                version, propio, enemigo);
    }

    void hacerColocacion(Coordenada proa, Orientacion orientacion) throws BatallaException {
        propio.colocarSiguienteBarco(proa, orientacion);
    }

    void prepararCombate() throws BatallaException {
        if (!propio.estaPreparado()) { throw new BatallaException("Debe posicionar todos sus barcos primero"); }
        if (!enemigo.estaPreparado()) {
            enemigo = despliegue.completar(enemigo);
        }
    }

    SituacionBatalla hacerRonda(int rondaEsperada, Coordenada destino) throws BatallaException {
        if (rondaEsperada != ronda) {
            throw new BatallaException("La ronda indicada ya no es la actual; consulte la batalla antes de disparar");
        }
        Oceano siguienteEnemigo = enemigo.copiar();
        siguienteEnemigo.recibirDisparo(destino);
        Oceano siguientePropio = propio;
        // Se confirma la ronda recién después de que los dos disparos aplicables resultaron válidos.
        if (!siguienteEnemigo.estaHundida()) {
            siguientePropio = propio.copiar();
            InformacionDisparos informacion = new InformacionDisparos(propio.getAncho(), propio.getAlto(), propio.getDisparos());
            siguientePropio.recibirDisparo(tactica.elegirDisparo(informacion));
        }
        return confirmarRonda(siguientePropio, siguienteEnemigo);
    }

    private SituacionBatalla confirmarRonda(Oceano siguientePropio, Oceano siguienteEnemigo) {
        propio = siguientePropio;
        enemigo = siguienteEnemigo;
        if (enemigo.estaHundida()) {
            return SituacionBatalla.GANADA;
        }
        if (propio.estaHundida()) { return SituacionBatalla.PERDIDA; }
        ronda++;
        return SituacionBatalla.EN_COMBATE;
    }
}
