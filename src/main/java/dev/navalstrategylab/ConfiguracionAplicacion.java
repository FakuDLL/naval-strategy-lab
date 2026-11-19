package dev.navalstrategylab;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.navalstrategylab.excepciones.BatallaException;
import dev.navalstrategylab.modelo.DatosIniciales;
import dev.navalstrategylab.modelo.Fachada;

@org.springframework.scheduling.annotation.EnableScheduling
@Configuration
public class ConfiguracionAplicacion {
    @Bean
    public Fachada fachada() throws BatallaException {
        return new Fachada(DatosIniciales.crearSistema());
    }
}
