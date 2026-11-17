package uy.edu.um.catalogo.web;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uy.edu.um.catalogo.core.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogoIT {
    static final Path ARCHIVO = Path.of("target", "it-" + UUID.randomUUID(), "catalogo.json").toAbsolutePath();
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r) {
        r.add("catalogo.archivo", () -> ARCHIVO.toString());
        r.add("catalogo.revision", () -> "integracion");
    }
    @Autowired TestRestTemplate http;

    @Test void registroHttpIntegraWebServicioYDisco() throws Exception {
        var datos = Map.of("nombre", "Juego " + UUID.randomUUID(), "genero", "AVENTURA", "imagen", "/img/aventura.svg");
        var respuesta = http.postForEntity("/api/videojuegos", datos, Videojuego.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Videojuego juego = respuesta.getBody();
        assertThat(juego.nombre()).isEqualTo(datos.get("nombre"));
        assertThat(http.getForObject("/api/videojuegos?genero=AVENTURA", Videojuego[].class)).contains(juego);
        assertThat(new JsonCatalogoRepository(ARCHIVO).listar()).contains(juego);
        assertThat(Files.readString(ARCHIVO)).contains(datos.get("nombre"));
    }
    @Test void duplicadoDevuelve409() {
        var datos = Map.of("nombre", "Duplicado " + UUID.randomUUID(), "genero", "ACCION");
        assertThat(http.postForEntity("/api/videojuegos", datos, String.class).getStatusCode().value()).isEqualTo(201);
        assertThat(http.postForEntity("/api/videojuegos", datos, String.class).getStatusCode().value()).isEqualTo(409);
    }
    @Test void rechazaNombreEnBlanco() {
        var respuesta = http.postForEntity("/api/videojuegos", Map.of("nombre", " ", "genero", "ROL"), String.class);
        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody()).contains("nombre");
    }
    @Test void rechazaGeneroDesconocidoEnCuerpoYFiltro() {
        assertThat(http.postForEntity("/api/videojuegos", Map.of("nombre", "X", "genero", "INVENTADO"), String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(http.getForEntity("/api/videojuegos?genero=INVENTADO", String.class).getStatusCode().value()).isEqualTo(400);
    }
    @Test void rechazaJsonRotoYCuerpoVacio() {
        var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        for (String cuerpo : new String[]{"{roto", "null", "{}", ""}) {
            assertThat(http.postForEntity("/api/videojuegos", new HttpEntity<>(cuerpo, headers), String.class).getStatusCode().value()).isEqualTo(400);
        }
    }
    @Test void sirvePortadaImagenYEstado() {
        var portada = http.getForEntity("/", String.class);
        assertThat(portada.getStatusCode().value()).isEqualTo(200);
        assertThat(portada.getBody()).contains("formulario", "app.js", "Filtrar por género");
        assertThat(http.getForObject("/img/mando.svg", String.class)).contains("<svg");
        assertThat(http.getForObject("/api/estado", String.class)).contains("OK", "integracion");
        assertThat(http.getForObject("/api/generos", String[].class)).contains("ACCION", "AVENTURA", "ESTRATEGIA", "DEPORTES", "ROL");
    }
    @Test void noAceptaImagenFueraDeCatalogo() {
        assertThat(http.postForEntity("/api/videojuegos", Map.of("nombre", "X", "genero", "ROL", "imagen", "https://externo.test/img.png"), String.class).getStatusCode().value()).isEqualTo(400);
    }
}
