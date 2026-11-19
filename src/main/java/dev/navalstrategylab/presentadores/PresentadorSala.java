package dev.navalstrategylab.presentadores;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.presentacion.*;

@RestController
@RequestMapping("/api")
public class PresentadorSala {

    private final Vistas vistas;

    public PresentadorSala(Vistas vistas) {
        this.vistas = vistas;
    }

    @PostMapping("/abrir")
    public Commands abrir() {
        return vistas.abrir();
    }

    @GetMapping(value = "/eventos", produces = "text/event-stream")
    public SseEmitter eventos(@RequestParam String vista) throws BatallaException {
        return vistas.conectar(vista);
    }

    @PostMapping("/recuperar")
    public Commands recuperar(@RequestParam String vista, @RequestParam(defaultValue = "") String nombre)
        throws BatallaException {
        return vistas.recuperar(vista, nombre);
    }

    @PostMapping("/cerrar")
    public void cerrar(@RequestParam String vista) {
        vistas.cerrar(vista);
    }

    @PostMapping("/sala")
    public Commands sala(@RequestParam String vista) throws BatallaException {
        return vistas.navegar(vista, "sala", null);
    }

    @PostMapping("/entrar")
    public Commands entrar(@RequestParam String vista, @RequestParam String nombre) throws BatallaException {
        return vistas.navegar(vista, "batalla", nombre);
    }

    @PostMapping("/nueva")
    public Commands nueva(@RequestParam String vista) throws BatallaException {
        return vistas.navegar(vista, "nueva", null);
    }
}
