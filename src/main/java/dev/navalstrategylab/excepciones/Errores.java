package dev.navalstrategylab.excepciones;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class Errores {

    @ExceptionHandler(BatallaException.class)
    public ResponseEntity<String> dominio(BatallaException e) {
        return respuesta(e.getMessage());
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
    })
    public ResponseEntity<String> parametros(Exception e) {
        return respuesta(
            "Revise los datos ingresados: las dimensiones y cantidades deben ser números enteros y las opciones deben ser válidas"
        );
    }

    private ResponseEntity<String> respuesta(String mensaje) {
        return ResponseEntity.status(299)
            .header("Content-Type", "text/plain;charset=UTF-8")
            .body(mensaje);
    }
}
