package uy.edu.um.catalogo.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import uy.edu.um.catalogo.core.*;

class CatalogoControllerTest {
    @Test void registroDelegaAlModuloCoreYDevuelve201() {
        var servicio = mock(CatalogoService.class);
        var juego = new Videojuego(1, "Celeste", Genero.AVENTURA, "/img/mando.svg");
        when(servicio.registrar("Celeste", Genero.AVENTURA, null)).thenReturn(juego);
        var controlador = new CatalogoController(servicio, "Catálogo", "abc");
        var respuesta = controlador.registrar(new CatalogoController.Registro("Celeste", Genero.AVENTURA, null));
        assertThat(respuesta.getStatusCode().value()).isEqualTo(201);
        assertThat(respuesta.getBody()).isEqualTo(juego);
        verify(servicio).registrar("Celeste", Genero.AVENTURA, null);
    }
    @Test void filtroDelegaAlServicio() {
        var servicio = mock(CatalogoService.class);
        var controlador = new CatalogoController(servicio, "Catálogo", "abc");
        controlador.listar(Genero.ROL);
        verify(servicio).listar(Genero.ROL);
    }
    @Test void estadoPublicaRevisionDelArtefacto() {
        var controlador = new CatalogoController(mock(CatalogoService.class), "Mensaje", "abc123");
        assertThat(controlador.estado()).containsEntry("estado", "OK").containsEntry("revision", "abc123").containsEntry("mensaje", "Mensaje");
        assertThat(controlador.generos()).containsExactly(Genero.values());
    }
}
