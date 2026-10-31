package br.com.ecociente.mobile.config.validador;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import br.com.ecociente.mobile.core.domain.validador.DadosTriagem;
import br.com.ecociente.mobile.core.domain.validador.StatusTriagem;
import br.com.ecociente.mobile.core.domain.validador.VeredictoFoto;

class ValidadorJacksonConfigTest {

    private ObjectMapper mapper;

    @BeforeEach
    void preparar() {
        var builder = new Jackson2ObjectMapperBuilder();

        new ValidadorJacksonConfig()
                .validadorJsonCustomizer()
                .customize(builder);

        mapper = builder.build();
    }

    @Test
    void devePreservarCamposSnakeCaseDoVeredicto() throws Exception {
        var veredicto = new VeredictoFoto(
                StatusTriagem.OK,
                true,
                "plastico",
                "plastico",
                0.95,
                "hash-teste",
                List.of(),
                List.of(),
                "Foto aprovada");

        var json = mapper.readTree(
                mapper.writeValueAsString(veredicto));

        assertEquals("ok", json.get("status").asText());
        assertTrue(json.get("pertinente").asBoolean());

        assertEquals(
                "plastico",
                json.get("categoria_informada").asText());

        assertEquals(
                "plastico",
                json.get("categoria_detectada").asText());

        assertEquals("hash-teste", json.get("hash_foto").asText());

        assertFalse(json.has("categoriaInformada"));
        assertFalse(json.has("categoriaDetectada"));
        assertFalse(json.has("hashFoto"));
    }

    @Test
    void deveSerializarTodosOsStatusEmMinusculas() throws Exception {
        assertEquals(
                "\"ok\"",
                mapper.writeValueAsString(StatusTriagem.OK));

        assertEquals(
                "\"pedido_invalido\"",
                mapper.writeValueAsString(StatusTriagem.PEDIDO_INVALIDO));

        assertEquals(
                "\"foto_indisponivel\"",
                mapper.writeValueAsString(StatusTriagem.FOTO_INDISPONIVEL));
    }

    @Test
    void deveConverterDadosTriagemEmSnakeCase() throws Exception {
        var dados = new DadosTriagem(
                "https://storage.teste/foto.jpg",
                "Plástico");

        var json = mapper.readTree(
                mapper.writeValueAsString(dados));

        assertEquals(
                "https://storage.teste/foto.jpg",
                json.get("url_foto").asText());

        assertEquals(
                "Plástico",
                json.get("nome_categoria").asText());

        assertFalse(json.has("urlFoto"));
        assertFalse(json.has("nomeCategoria"));
    }

    @Test
    void deveLerDadosTriagemEmSnakeCase() throws Exception {
        var json = """
                {
                  "url_foto": "https://storage.teste/foto.jpg",
                  "nome_categoria": "Plástico"
                }
                """;

        var dados = mapper.readValue(json, DadosTriagem.class);

        assertEquals(
                "https://storage.teste/foto.jpg",
                dados.urlFoto());

        assertEquals("Plástico", dados.nomeCategoria());
    }
}