package uy.edu.um.catalogo.core;

public class NombreDuplicadoException extends RuntimeException {
    public NombreDuplicadoException() {
        super("Ya existe un videojuego con ese nombre.");
    }
}
