package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.ecociente.mobile.core.service.CursoService;
import br.com.ecociente.mobile.dataprovider.entity.CursoEntity;
import br.com.ecociente.mobile.dataprovider.gateway.CursoGatewayImpl;
import br.com.ecociente.mobile.dataprovider.mapper.CursoEntityMapper;
import br.com.ecociente.mobile.dataprovider.repository.CursoRepository;
import br.com.ecociente.mobile.entrypoint.controller.CursoController;
import br.com.ecociente.mobile.entrypoint.mapper.CursoMapper;

@Tag("integracao")
@Testcontainers
@SpringBootTest(
    classes = CursoIntegrationTest.Configuracao.class,
    properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false",
        "spring.jpa.open-in-view=false"
    }
)
@AutoConfigureMockMvc(addFilters = false)
@Sql("/sql/cursos-test.sql")
class CursoIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mobile_cursos_test")
            .withUsername("teste")
            .withPassword("teste");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveListarSomenteCursosAtivosEmOrdemDeTituloEId()
        throws Exception {

        mvc.perform(get("/api/cursos"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
            ))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].titulo").value("Compostagem"))
            .andExpect(jsonPath("$[1].id").value(1))
            .andExpect(jsonPath("$[1].titulo").value("Reciclagem"))
            .andExpect(jsonPath("$[2].id").value(4))
            .andExpect(jsonPath("$[2].titulo").value("Reciclagem"))
            .andExpect(jsonPath("$[0].estaAtivo").doesNotExist());
    }

    @Test
    void deveBuscarCursoAtivoComTodosOsCamposDaResposta()
        throws Exception {

        mvc.perform(get("/api/cursos/1"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                    "id": 1,
                    "titulo": "Reciclagem",
                    "descricao": "Como separar resíduos."
                }
                """, true));
    }

    @Test
    void devePermitirCursoSemDescricao() throws Exception {
        mvc.perform(get("/api/cursos/2"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                    "id": 2,
                    "titulo": "Compostagem",
                    "descricao": null
                }
                """, true));
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 999, 0, -1})
    void deveRetornar404ParaCursoIndisponivel(int cursoId)
        throws Exception {

        mvc.perform(get("/api/cursos/{cursoId}", cursoId))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));
    }

    @Test
    void deveRetornarListaVaziaQuandoTodosOsCursosEstiveremInativos()
        throws Exception {

        jdbc.update("UPDATE tb_cursos SET esta_ativo = FALSE");

        mvc.perform(get("/api/cursos"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]", true));
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistiremCursos()
        throws Exception {

        jdbc.update("DELETE FROM tb_cursos");

        mvc.perform(get("/api/cursos"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]", true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "2147483648"})
    void deveRetornar400QuandoIdNaoPuderSerConvertidoParaInteger(
        String cursoId
    ) throws Exception {

        mvc.perform(get("/api/cursos/{cursoId}", cursoId))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveRefletirDesativacaoDoCursoNasDuasConsultas()
        throws Exception {

        jdbc.update(
            "UPDATE tb_cursos SET esta_ativo = FALSE WHERE id_curso = ?",
            1
        );

        mvc.perform(get("/api/cursos/1"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/api/cursos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[1].id").value(4));
    }

    @Test
    void deveRetornarDescricaoLongaSemTruncarConteudo()
        throws Exception {

        String descricao = "Educação ambiental. ".repeat(500);

        jdbc.update(
            "UPDATE tb_cursos SET descricao_curso = ? WHERE id_curso = ?",
            descricao,
            1
        );

        mvc.perform(get("/api/cursos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.descricao").value(descricao));
    }

    @Test
    void consultasNaoDevemAlterarOsRegistros() throws Exception {
        var antes = jdbc.queryForList(
            "SELECT * FROM tb_cursos ORDER BY id_curso"
        );

        mvc.perform(get("/api/cursos"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/cursos/1"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/cursos/3"))
            .andExpect(status().isNotFound());

        var depois = jdbc.queryForList(
            "SELECT * FROM tb_cursos ORDER BY id_curso"
        );

        assertEquals(antes, depois);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = CursoEntity.class)
    @EnableJpaRepositories(basePackageClasses = CursoRepository.class)
    @Import({
        CursoController.class,
        CursoService.class,
        CursoMapper.class,
        CursoGatewayImpl.class,
        CursoEntityMapper.class
    })
    static class Configuracao {
    }
}