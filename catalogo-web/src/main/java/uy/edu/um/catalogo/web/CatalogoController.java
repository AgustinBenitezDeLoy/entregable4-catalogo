package uy.edu.um.catalogo.web;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.catalogo.core.*;

@RestController
@RequestMapping("/api")
public class CatalogoController {
    private final CatalogoService service;
    private final String mensaje;
    private final String revision;

    public CatalogoController(CatalogoService service,
            @Value("${catalogo.mensaje}") String mensaje,
            @Value("${catalogo.revision:desarrollo}") String revision) {
        this.service = service;
        this.mensaje = mensaje;
        this.revision = revision;
    }

    public record Registro(String nombre, Genero genero, String imagen) { }

    @GetMapping("/videojuegos")
    public List<Videojuego> listar(@RequestParam(required = false) Genero genero) {
        return service.listar(genero);
    }

    @PostMapping("/videojuegos")
    public ResponseEntity<Videojuego> registrar(@RequestBody Registro registro) {
        return ResponseEntity.status(201).body(service.registrar(registro.nombre(), registro.genero(), registro.imagen()));
    }

    @GetMapping("/generos")
    public Genero[] generos() { return Genero.values(); }

    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of("estado", "OK", "mensaje", mensaje, "revision", revision);
    }
}
