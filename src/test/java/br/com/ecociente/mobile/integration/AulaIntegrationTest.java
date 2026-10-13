package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.ecociente.mobile.core.service.AulaService;
import br.com.ecociente.mobile.dataprovider.entity.AulaEntity;
import br.com.ecociente.mobile.dataprovider.entity.CursoEntity;
import br.com.ecociente.mobile.dataprovider.gateway.AulaGatewayImpl;
import br.com.ecociente.mobile.dataprovider.gateway.CursoGatewayImpl;
import br.com.ecociente.mobile.dataprovider.mapper.AulaEntityMapper;
import br.com.ecociente.mobile.dataprovider.mapper.CursoEntityMapper;
import br.com.ecociente.mobile.dataprovider.repository.AulaRepository;
import br.com.ecociente.mobile.entrypoint.controller.AulaController;
import br.com.ecociente.mobile.entrypoint.mapper.AulaMapper;

@Tag("integracao")
@Testcontainers
@SpringBootTest(
    classes = AulaIntegrationTest.Configuracao.class,
    properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false",
        "spring.jpa.open-in-view=false"
    }
)
@AutoConfigureMockMvc(addFilters = false)
@Sql("/sql/aulas-test.sql")
class AulaIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mobile_aulas_test")
            .withUsername("teste")
            .withPassword("teste");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveListarAulasNaOrdemDefinidaNoCurso() throws Exception {
        mvc.perform(get("/api/cursos/1/aulas"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
            ))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(11))
            .andExpect(jsonPath("$[0].ordem").value(1))
            .andExpect(jsonPath("$[1].id").value(10))
            .andExpect(jsonPath("$[1].ordem").value(2))
            .andExpect(jsonPath("$[2].id").value(12))
            .andExpect(jsonPath("$[2].ordem").value(3));
    }

    @Test
    void deveRetornarSomenteAulasDoCursoSolicitado() throws Exception {
        mvc.perform(get("/api/cursos/2/aulas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(20))
            .andExpect(jsonPath("$[0].cursoId").value(2));
    }

    @Test
    void deveBuscarAulaComTodosOsCampos() throws Exception {
        mvc.perform(get("/api/aulas/10"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                    "id": 10,
                    "cursoId": 1,
                    "titulo": "Separação",
                    "conteudo": "Separe os resíduos por categoria.",
                    "ordem": 2
                }
                """, true));
    }

    @Test
    void devePermitirConteudoNulo() throws Exception {
        mvc.perform(get("/api/aulas/12"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                    "id": 12,
                    "cursoId": 1,
                    "titulo": "Revisão",
                    "conteudo": null,
                    "ordem": 3
                }
                """, true));
    }

    @Test
    void deveRetornarListaVaziaParaCursoAtivoSemAulas() throws Exception {
        mvc.perform(get("/api/cursos/4/aulas"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]", true));
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 999, 0, -1})
    void deveRetornar404ParaCursoIndisponivel(int cursoId)
        throws Exception {

        mvc.perform(get("/api/cursos/{cursoId}/aulas", cursoId))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 999, 0, -1})
    void deveRetornar404ParaAulaIndisponivel(int aulaId)
        throws Exception {

        mvc.perform(get("/api/aulas/{aulaId}", aulaId))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "2147483648"})
    void deveRejeitarIdQueNaoPodeSerConvertidoParaInteger(String id)
        throws Exception {

        mvc.perform(get("/api/aulas/{aulaId}", id))
            .andExpect(status().isBadRequest());

        mvc.perform(get("/api/cursos/{cursoId}/aulas", id))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveBloquearConsultasAposDesativacaoDoCurso() throws Exception {
        jdbc.update(
            "UPDATE tb_cursos SET esta_ativo = FALSE WHERE id_curso = ?",
            1
        );

        mvc.perform(get("/api/cursos/1/aulas"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/api/aulas/10"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/api/aulas/20"))
            .andExpect(status().isOk());
    }

    @Test
    void devePreservarConteudoLongoComAcentosEQuebrasDeLinha()
        throws Exception {

        String conteudo = "Educação ambiental: ação e consciência.\n"
            .repeat(500);

        jdbc.update(
            "UPDATE tb_aulas SET conteudo_aula = ? WHERE id_aula = ?",
            conteudo,
            10
        );

        mvc.perform(get("/api/aulas/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conteudo").value(conteudo));

        mvc.perform(get("/api/cursos/1/aulas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[1].conteudo").value(conteudo));
    }

    @Test
    void consultasNaoDevemAlterarCursosOuAulas() throws Exception {
        var cursosAntes = jdbc.queryForList(
            "SELECT * FROM tb_cursos ORDER BY id_curso"
        );
        var aulasAntes = jdbc.queryForList(
            "SELECT * FROM tb_aulas ORDER BY id_aula"
        );

        mvc.perform(get("/api/cursos/1/aulas"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/aulas/10"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/aulas/30"))
            .andExpect(status().isNotFound());

        assertEquals(
            cursosAntes,
            jdbc.queryForList("SELECT * FROM tb_cursos ORDER BY id_curso")
        );
        assertEquals(
            aulasAntes,
            jdbc.queryForList("SELECT * FROM tb_aulas ORDER BY id_aula")
        );
    }

    @Test
    void bancoDeveImpedirOrdemDuplicadaDentroDoMesmoCurso() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbc.update("""
                INSERT INTO tb_aulas (curso_id, titulo_aula, ordem)
                VALUES (1, 'Ordem duplicada', 1)
                """)
        );

        assertEquals(
            3,
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM tb_aulas WHERE curso_id = 1",
                Integer.class
            )
        );
    }

    @Test
    void bancoDeveImpedirAulaVinculadaACursoInexistente() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbc.update("""
                INSERT INTO tb_aulas (curso_id, titulo_aula, ordem)
                VALUES (999, 'Curso inexistente', 1)
                """)
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
        AulaEntity.class,
        CursoEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = AulaRepository.class)
    @Import({
        AulaController.class,
        AulaService.class,
        AulaMapper.class,
        AulaGatewayImpl.class,
        AulaEntityMapper.class,
        CursoGatewayImpl.class,
        CursoEntityMapper.class
    })
    static class Configuracao {
    }
}