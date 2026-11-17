package uy.edu.um.catalogo.core;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CatalogoServiceTest {
    @TempDir Path dir;
    CatalogoService service;
    @BeforeEach void crear() { service = new CatalogoService(new JsonCatalogoRepository(dir.resolve("juegos.json"))); }

    @Test void registraNormalizandoNombreYUsaImagenPredeterminada() {
        Videojuego juego = service.registrar("  Celeste  ", Genero.AVENTURA, null);
        assertThat(juego).isEqualTo(new Videojuego(1, "Celeste", Genero.AVENTURA, "/img/mando.svg"));
        assertThat(service.listar(null)).containsExactly(juego);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\t\n"})
    void rechazaNombreVacio(String nombre) {
        assertThatIllegalArgumentException().isThrownBy(() -> service.registrar(nombre, Genero.ACCION, null));
        assertThat(service.listar(null)).isEmpty();
    }
    @Test void validaLimiteOchenta() {
        service.registrar("a".repeat(80), Genero.ROL, null);
        assertThatIllegalArgumentException().isThrownBy(() -> service.registrar("a".repeat(81), Genero.ROL, null));
    }
    @Test void rechazaGeneroNulo() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.registrar("Celeste", null, null));
    }
    @Test void rechazaImagenExterna() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.registrar("Celeste", Genero.ROL, "javascript:alert(1)"));
    }
    @Test void evitaDuplicadosSinDistinguirMayusculas() {
        service.registrar("Celeste", Genero.AVENTURA, null);
        assertThatThrownBy(() -> service.registrar(" CELESTE ", Genero.ACCION, null)).isInstanceOf(NombreDuplicadoException.class);
        assertThat(service.listar(null)).hasSize(1);
    }
    @Test void filtraPorGeneroSinModificarCatalogo() {
        Videojuego aventura = service.registrar("Celeste", Genero.AVENTURA, null);
        service.registrar("Ajedrez", Genero.ESTRATEGIA, "/img/estrategia.svg");
        assertThat(service.listar(Genero.AVENTURA)).containsExactly(aventura);
        assertThat(service.listar(Genero.DEPORTES)).isEmpty();
        assertThat(service.listar(null)).hasSize(2);
    }
}
