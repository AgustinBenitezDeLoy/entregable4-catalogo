package uy.edu.um.catalogo.web;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uy.edu.um.catalogo.core.CatalogoRepository;
import uy.edu.um.catalogo.core.JsonCatalogoRepository;

@Configuration
public class CatalogoConfig {
    @Bean
    CatalogoRepository catalogoRepository(@Value("${catalogo.archivo:data/catalogo.json}") String archivo) {
        return new JsonCatalogoRepository(Path.of(archivo));
    }
}
