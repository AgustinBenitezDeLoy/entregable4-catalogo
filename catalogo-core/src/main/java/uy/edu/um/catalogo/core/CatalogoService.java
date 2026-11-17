package uy.edu.um.catalogo.core;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogoService {
    private final CatalogoRepository repository;

    public CatalogoService(CatalogoRepository repository) {
        this.repository = repository;
    }

    public Videojuego registrar(String nombre, Genero genero, String imagen) {
        if (nombre == null || nombre.isEmpty() || nombre.strip().length() > 80) {
            throw new IllegalArgumentException("El nombre debe tener entre 1 y 80 caracteres.");
        }
        if (genero == null) {
            throw new IllegalArgumentException("El género es obligatorio.");
        }
        String ruta = imagen == null || imagen.isBlank() ? "/img/mando.svg" : imagen.strip();
        if (!List.of("/img/mando.svg", "/img/aventura.svg", "/img/estrategia.svg").contains(ruta)) {
            throw new IllegalArgumentException("Seleccioná una imagen del catálogo.");
        }
        return repository.agregar(nombre.strip(), genero, ruta);
    }

    public List<Videojuego> listar(Genero genero) {
        return repository.listar().stream().filter(j -> genero == null || j.genero() == genero).toList();
    }
}
