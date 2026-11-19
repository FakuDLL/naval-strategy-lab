package dev.navalstrategylab.modelo;

/** Los datos de una misma versión se capturan juntos, nunca a mitad de ronda. */
public final class InstantaneaBatalla {
    private final String nombre;
    private final String tactica;
    private final SituacionBatalla situacion;
    private final int ronda;
    private final long version;
    private final InstantaneaOceano propio;
    private final InstantaneaOceano enemigo;

    InstantaneaBatalla(String nombre, String tactica, SituacionBatalla situacion, int ronda,
            long version, Oceano propio, Oceano enemigo) {
        this.nombre = nombre;
        this.tactica = tactica;
        this.situacion = situacion;
        this.ronda = ronda;
        this.version = version;
        this.propio = new InstantaneaOceano(propio, true);
        this.enemigo = new InstantaneaOceano(enemigo, situacion == SituacionBatalla.PERDIDA);
    }

    public String getNombre() { return nombre; }
    public String getTactica() { return tactica; }
    public SituacionBatalla getSituacion() { return situacion; }
    public int getRonda() { return ronda; }
    public long getVersion() { return version; }
    public InstantaneaOceano getPropio() { return propio; }
    public InstantaneaOceano getEnemigo() { return enemigo; }
}
