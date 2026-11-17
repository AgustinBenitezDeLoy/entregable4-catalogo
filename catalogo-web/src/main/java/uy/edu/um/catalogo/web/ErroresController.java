package uy.edu.um.catalogo.web;

import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uy.edu.um.catalogo.core.NombreDuplicadoException;

@RestControllerAdvice
public class ErroresController {
    @ExceptionHandler(NombreDuplicadoException.class)
    ResponseEntity<Map<String, String>> duplicado(NombreDuplicadoException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> formato(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Solicitud inválida. Verificá el JSON y el género."));
    }

    @ExceptionHandler(UncheckedIOException.class)
    ResponseEntity<Map<String, String>> persistencia(UncheckedIOException e) {
        return ResponseEntity.status(503).body(Map.of("error", "No se pudo guardar. Revisá el almacenamiento e intentá de nuevo."));
    }
}
