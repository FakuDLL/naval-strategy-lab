package dev.navalstrategylab.presentadores;

import org.springframework.web.bind.annotation.*;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.*;
import dev.navalstrategylab.presentacion.*;

@RestController
@RequestMapping("/api/combate")
public class PresentadorCombate {

    private final Fachada fachada;
    private final Vistas vistas;

    public PresentadorCombate(Fachada fachada, Vistas vistas) {
        this.fachada = fachada;
        this.vistas = vistas;
    }

    @PostMapping("/disparar")
    public Commands disparar(
        @RequestParam String vista,
        @RequestParam String nombre,
        @RequestParam int ronda,
        @RequestParam int coordenadaVertical,
        @RequestParam int coordenadaHorizontal
    ) throws BatallaException {
        vistas.exigirBatalla(vista, nombre);
        fachada.disparar(nombre, ronda, new Coordenada(coordenadaVertical, coordenadaHorizontal));
        return vistas.actualizar(vista);
    }
}
