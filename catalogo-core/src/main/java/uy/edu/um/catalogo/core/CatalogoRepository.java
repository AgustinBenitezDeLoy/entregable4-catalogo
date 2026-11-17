package uy.edu.um.catalogo.core;

import java.util.List;

public interface CatalogoRepository {
    List<Videojuego> listar();
    Videojuego agregar(String nombre, Genero genero, String imagen);
}
