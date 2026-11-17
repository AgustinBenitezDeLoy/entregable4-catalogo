package uy.edu.um.catalogo.core;

import static org.assertj.core.api.Assertions.*;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonCatalogoRepositoryTest {
    @TempDir Path dir;
    @Test void conservaRegistrosEIdsAlReabrir() {
        Path archivo = dir.resolve("data/juegos.json");
        var primero = new JsonCatalogoRepository(archivo);
        primero.agregar("Celeste", Genero.AVENTURA, "/img/mando.svg");
        var segundo = new JsonCatalogoRepository(archivo);
        assertThat(segundo.listar()).extracting(Videojuego::nombre).containsExactly("Celeste");
        assertThat(segundo.agregar("Hades", Genero.ACCION, "/img/mando.svg").id()).isEqualTo(2);
    }
    @Test void noSobrescribeArchivoCorrupto() throws Exception {
        Path archivo = dir.resolve("juegos.json");
        Files.writeString(archivo, "{roto");
        assertThatThrownBy(() -> new JsonCatalogoRepository(archivo)).isInstanceOf(UncheckedIOException.class);
        assertThat(Files.readString(archivo)).isEqualTo("{roto");
    }
    @Test void rechazaContenidoNulo() throws Exception {
        Path archivo = dir.resolve("juegos.json");
        Files.writeString(archivo, "null");
        assertThatThrownBy(() -> new JsonCatalogoRepository(archivo)).isInstanceOf(IllegalStateException.class);
    }
    @Test void falloDeDiscoNoCambiaEstadoEnMemoria() throws Exception {
        Path padre = dir.resolve("bloqueo");
        Files.writeString(padre, "archivo que impide crear directorio");
        var repo = new JsonCatalogoRepository(padre.resolve("juegos.json"));
        assertThatThrownBy(() -> repo.agregar("Celeste", Genero.AVENTURA, "/img/mando.svg")).isInstanceOf(UncheckedIOException.class);
        assertThat(repo.listar()).isEmpty();
    }
    @Test void listadoEsInmutable() {
        var repo = new JsonCatalogoRepository(dir.resolve("juegos.json"));
        assertThatThrownBy(() -> repo.listar().add(new Videojuego(1, "X", Genero.ROL, "/img/mando.svg"))).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void escriturasConcurrentesNoPierdenRegistros() throws Exception {
        Path archivo = dir.resolve("juegos.json");
        var repo = new JsonCatalogoRepository(archivo);
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tareas = java.util.stream.IntStream.range(0, 20).<java.util.concurrent.Callable<Videojuego>>mapToObj(i ->
                    () -> repo.agregar("Juego " + i, Genero.ACCION, "/img/mando.svg")).toList();
            for (var resultado : executor.invokeAll(tareas)) resultado.get();
        }
        assertThat(new JsonCatalogoRepository(archivo).listar()).hasSize(20).extracting(Videojuego::id).doesNotHaveDuplicates();
    }
}
