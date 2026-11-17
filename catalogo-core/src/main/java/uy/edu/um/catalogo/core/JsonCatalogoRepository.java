package uy.edu.um.catalogo.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Archivo de una sola instancia: escritura atómica antes de cambiar el estado en memoria. */
public class JsonCatalogoRepository implements CatalogoRepository {
    private final Path archivo;
    private final ObjectMapper mapper = new ObjectMapper();
    private List<Videojuego> juegos;

    public JsonCatalogoRepository(Path archivo) {
        this.archivo = archivo.toAbsolutePath().normalize();
        try {
            juegos = Files.exists(this.archivo)
                    ? mapper.readValue(this.archivo.toFile(), new TypeReference<List<Videojuego>>() { })
                    : new ArrayList<>();
            if (juegos == null || juegos.stream().anyMatch(j -> j == null || j.id() < 1
                    || j.nombre() == null || j.genero() == null || j.imagen() == null)) {
                throw new IllegalStateException("El catálogo persistido contiene datos inválidos.");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el catálogo. No se sobrescribirá el archivo.", e);
        }
    }

    @Override
    public synchronized List<Videojuego> listar() {
        return List.copyOf(juegos);
    }

    @Override
    public synchronized Videojuego agregar(String nombre, Genero genero, String imagen) {
        if (juegos.stream().anyMatch(j -> j.nombre().equalsIgnoreCase(nombre))) {
            throw new NombreDuplicadoException();
        }
        long id = juegos.stream().mapToLong(Videojuego::id).max().orElse(0) + 1;
        Videojuego juego = new Videojuego(id, nombre, genero, imagen);
        List<Videojuego> nuevos = new ArrayList<>(juegos);
        nuevos.add(juego);
        persistir(nuevos);
        juegos = nuevos;
        return juego;
    }

    private void persistir(List<Videojuego> nuevos) {
        Path temporal = null;
        try {
            Files.createDirectories(archivo.getParent());
            temporal = Files.createTempFile(archivo.getParent(), "catalogo-", ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporal.toFile(), nuevos);
            try {
                Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el videojuego.", e);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); } catch (IOException ignored) { /* Error original prioritario. */ }
            }
        }
    }
}
