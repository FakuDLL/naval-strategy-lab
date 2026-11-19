package dev.navalstrategylab.presentadores;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.*;
import dev.navalstrategylab.presentacion.*;

@RestController
@RequestMapping("/api/preparacion")
public class PresentadorPreparacion {

    private final Fachada fachada;
    private final Vistas vistas;

    public PresentadorPreparacion(Fachada fachada, Vistas vistas) {
        this.fachada = fachada;
        this.vistas = vistas;
    }

    @PostMapping("/crear")
    public Commands crear(
        @RequestParam String vista,
        @RequestParam(defaultValue = "") String nombre,
        @RequestParam int ancho,
        @RequestParam int alto,
        @RequestParam String tactica,
        @RequestParam Map<String, String> parametros
    ) throws BatallaException {
        vistas.exigirVista(vista);
        Map<TipoBarco, Integer> cantidades = new EnumMap<>(TipoBarco.class);
        try {
            for (TipoBarco tipo : TipoBarco.values())
                cantidades.put(tipo, Integer.parseInt(parametros.getOrDefault(tipo.name(), "0")));
        } catch (NumberFormatException e) {
            throw new BatallaException("Las cantidades de barcos deben ser números enteros");
        }
        InstantaneaBatalla b = fachada.crearBatalla(nombre, ancho, alto, cantidades, tactica);
        return vistas.navegar(vista, "batalla", b.getNombre());
    }

    @PostMapping("/orientacion")
    public Commands orientar(
        @RequestParam String vista,
        @RequestParam String nombre,
        @RequestParam long entrada,
        @RequestParam Orientacion orientacion,
        @RequestParam(defaultValue = "0") long seleccionOrientacion
    ) throws BatallaException {
        vistas.orientar(vista, nombre, entrada, orientacion, seleccionOrientacion);
        return vistas.actualizar(vista);
    }

    @PostMapping("/celda")
    public Commands colocar(
        @RequestParam String vista,
        @RequestParam String nombre,
        @RequestParam long entrada,
        @RequestParam long version,
        @RequestParam int coordenadaVertical,
        @RequestParam int coordenadaHorizontal,
        @RequestParam Orientacion orientacion,
        @RequestParam(defaultValue = "0") long seleccionOrientacion
    ) throws BatallaException {
        vistas.orientar(vista, nombre, entrada, orientacion, seleccionOrientacion);
        fachada.colocarBarco(
            nombre,
            version,
            new Coordenada(coordenadaVertical, coordenadaHorizontal),
            orientacion
        );
        return vistas.actualizar(vista);
    }

    @PostMapping("/iniciar")
    public Commands iniciar(@RequestParam String vista, @RequestParam String nombre) throws BatallaException {
        vistas.exigirBatalla(vista, nombre);
        fachada.iniciarBatalla(nombre);
        return vistas.actualizar(vista);
    }
}
