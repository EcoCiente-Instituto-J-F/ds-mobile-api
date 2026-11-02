package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.ecociente.mobile.core.domain.educacao.RespostaQuiz;
import br.com.ecociente.mobile.core.exception.pontuacao.BusinessException;
import br.com.ecociente.mobile.core.service.educacao.FinalizarTentativaQuizService;
import br.com.ecociente.mobile.core.service.pontuacao.ConsolidarQuizInternoService;
import br.com.ecociente.mobile.core.service.pontuacao.PontuacaoService;
import br.com.ecociente.mobile.core.service.pontuacao.PostagemValidacaoService;

import br.com.ecociente.mobile.dataprovider.gateway.educacao.QuizGatewayImpl;
import br.com.ecociente.mobile.dataprovider.gateway.educacao.TentativaQuizGatewayImpl;
import br.com.ecociente.mobile.dataprovider.gateway.pontuacao.*;
import br.com.ecociente.mobile.dataprovider.mapper.educacao.QuizEntityMapper;
import br.com.ecociente.mobile.dataprovider.mapper.pontuacao.*;

@Tag("integracao")
@Testcontainers
@SpringJUnitConfig(PontuacaoFluxosReaisIntegrationTest.Config.class)
class PontuacaoFluxosReaisIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FinalizarTentativaQuizService finalizar;
    @Autowired private ConsolidarQuizInternoService consolidar;
    @Autowired private PontuacaoService pontuacao;
    @Autowired private PostagemValidacaoService validacao;

    @BeforeEach
    void preparar() throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {

            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");
            statement.execute(recurso("sql/ecociente-schema.sql"));
            statement.execute("SET search_path TO public");

            statement.execute("""
                    INSERT INTO tb_lkp_tipos_eventos_auditados (nome_evento)
                    SELECT nome FROM (
                        VALUES ('usuario_condominio'),
                               ('agendamento_coleta'),
                               ('postagem')
                    ) AS eventos(nome)
                    WHERE NOT EXISTS (
                        SELECT 1 FROM tb_lkp_tipos_eventos_auditados e
                        WHERE e.nome_evento = eventos.nome
                    );

                    INSERT INTO tb_lkp_tipos_operacoes_auditoria (nome_operacao)
                    SELECT nome FROM (
                        VALUES ('INSERT'), ('UPDATE'), ('DELETE')
                    ) AS operacoes(nome)
                    WHERE NOT EXISTS (
                        SELECT 1 FROM tb_lkp_tipos_operacoes_auditoria o
                        WHERE o.nome_operacao = operacoes.nome
                    );
                    """);

            statement.execute(recurso("sql/tentativas-quiz-dados-test.sql"));

            statement.execute("""
                    INSERT INTO tb_lkp_status_validacoes_postagens (
                        id_status_validacao, nome_status
                    )
                    OVERRIDING SYSTEM VALUE
                    VALUES
                        (1, 'em_analise'),
                        (2, 'aprovada'),
                        (3, 'reprovada');

                    INSERT INTO tb_lkp_categorias_residuos (
                        id_categoria, nome_categoria, pontos_base
                    )
                    OVERRIDING SYSTEM VALUE
                    VALUES (1, 'Plástico', 10);
                    """);
        }
    }

    @Test
    void deveCorrigirQuizECreditarUmaUnicaVez() {
        int tentativaId = criarTentativa();

        var resultado = finalizar.executar(
                20,
                tentativaId,
                List.of(new RespostaQuiz(1, 10), new RespostaQuiz(2, 20)));

        assertTrue(resultado.aprovado());
        assertEquals(2, resultado.acertos());

        var primeira = consolidar.executar(tentativaId, 20);
        var repetida = consolidar.executar(tentativaId, 20);

        assertEquals(primeira.getId(), repetida.getId());
        assertEquals(Integer.valueOf(30), primeira.getPontos());
        assertEquals(Integer.valueOf(1), primeira.getCondominioId());
        assertEquals(Integer.valueOf(1), primeira.getTorreId());

        assertEquals(
                Long.valueOf(1),
                jdbc.queryForObject("""
                        SELECT COUNT(*)
                        FROM tb_movimentacoes_pontos
                        WHERE tentativa_quiz_id = ?
                        """, Long.class, tentativaId));

        assertEquals(
                Long.valueOf(30),
                jdbc.queryForObject("""
                        SELECT SUM(pontos)
                        FROM tb_movimentacoes_pontos
                        WHERE tentativa_quiz_id = ?
                        """, Long.class, tentativaId));
    }

    @Test
    void quizReprovadoNaoDeveGerarMovimentacao() {
        int tentativaId = criarTentativa();

        var resultado = finalizar.executar(
                20,
                tentativaId,
                List.of(new RespostaQuiz(1, 11), new RespostaQuiz(2, 21)));

        assertFalse(resultado.aprovado());

        assertThrows(
                BusinessException.class,
                () -> consolidar.executar(tentativaId, 20));

        assertEquals(0L, totalMovimentacoes());
    }

    @Test
    void outroUsuarioNaoPodeConsolidarTentativa() {
        int tentativaId = criarTentativa();

        finalizar.executar(
                20,
                tentativaId,
                List.of(new RespostaQuiz(1, 10), new RespostaQuiz(2, 20)));

        assertThrows(
                BusinessException.class,
                () -> consolidar.executar(tentativaId, 30));

        assertEquals(0L, totalMovimentacoes());
    }

    @Test
    void aprovacaoDeDescarteDeveExecutarProcedureECreditar() {
        int postagemId = criarPostagem();

        validacao.decidir(postagemId, true);

        assertEquals(
                Boolean.TRUE,
                jdbc.queryForObject("""
                        SELECT pontuacao_ativa
                        FROM tb_postagens
                        WHERE id_postagem = ?
                        """, Boolean.class, postagemId));

        assertEquals(
                "aprovada",
                jdbc.queryForObject("""
                        SELECT s.nome_status
                        FROM tb_postagens p
                        JOIN tb_lkp_status_validacoes_postagens s
                          ON s.id_status_validacao = p.status_validacao_id
                        WHERE p.id_postagem = ?
                        """, String.class, postagemId));

        var credito = pontuacao.consolidarPostagem(
                postagemId, "retry-postagem-" + postagemId);

        assertNotNull(credito);
        assertEquals(Integer.valueOf(10), credito.getPontos());
        assertEquals(1L, totalMovimentacoes());

        assertEquals(
                Boolean.FALSE,
                jdbc.queryForObject("""
                        SELECT pontuacao_reconciliacao_pendente
                        FROM tb_postagens
                        WHERE id_postagem = ?
                        """, Boolean.class, postagemId));
    }

    @Test
    void recusaDeDescarteNaoDeveGerarCredito() {
        int postagemId = criarPostagem();

        validacao.decidir(postagemId, false);

        assertEquals(
                Boolean.FALSE,
                jdbc.queryForObject("""
                        SELECT pontuacao_ativa
                        FROM tb_postagens
                        WHERE id_postagem = ?
                        """, Boolean.class, postagemId));

        assertEquals(0L, totalMovimentacoes());
    }

    private int criarTentativa() {
        return jdbc.queryForObject("""
                INSERT INTO tb_tentativas_quiz (usuario_id, quiz_id)
                VALUES (20, 100)
                RETURNING id_tentativa
                """, Integer.class);
    }

    private int criarPostagem() {
        return jdbc.queryForObject("""
                INSERT INTO tb_postagens (
                    usuario_id, condominio_id, torre_id, categoria_id,
                    url_foto, hash_foto, capturada_em,
                    status_validacao_id, pontuacao_ativa
                )
                VALUES (
                    20, 1, 1, 1,
                    'https://storage.teste/foto.jpg',
                    'hash-fluxo-real',
                    now() - interval '5 minutes',
                    1, false
                )
                RETURNING id_postagem
                """, Integer.class);
    }

    private long totalMovimentacoes() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM tb_movimentacoes_pontos",
                Long.class);
    }

    private static String recurso(String caminho) throws Exception {
        try (var stream = new ClassPathResource(caminho).getInputStream()) {
            return new String(
                    stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(
            basePackages = "br.com.ecociente.mobile.dataprovider.repository")
    @Import({
            FinalizarTentativaQuizService.class,
            ConsolidarQuizInternoService.class,
            PontuacaoService.class,
            PostagemValidacaoService.class,
            QuizGatewayImpl.class,
            TentativaQuizGatewayImpl.class,
            QuizEntityMapper.class,
            PontuacaoCategoriaResiduoGatewayImpl.class,
            PontuacaoMovimentacaoPontosGatewayImpl.class,
            PontuacaoPostagemGatewayImpl.class,
            PontuacaoPostagemValidacaoGatewayImpl.class,
            PontuacaoQuizGatewayImpl.class,
            PontuacaoTentativaQuizGatewayImpl.class,
            ConfiancaPostagemGatewayImpl.class,
            PontuacaoCategoriaResiduoMapper.class,
            PontuacaoMovimentacaoPontosMapper.class,
            PontuacaoPostagemMapper.class,
            PontuacaoQuizMapper.class,
            PontuacaoTentativaQuizMapper.class
    })
    static class Config {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    POSTGRES.getJdbcUrl(),
                    POSTGRES.getUsername(),
                    POSTGRES.getPassword());
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        Clock clock() {
            return Clock.system(ZoneId.of("America/Sao_Paulo"));
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(
                DataSource dataSource) {

            var factory = new LocalContainerEntityManagerFactoryBean();

            factory.setDataSource(dataSource);
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setPackagesToScan(
                    "br.com.ecociente.mobile.dataprovider.entity");

            factory.setJpaPropertyMap(
                    java.util.Map.of("hibernate.hbm2ddl.auto", "none"));

            return factory;
        }

        @Bean
        EntityManager entityManager(EntityManagerFactory factory) {
            return SharedEntityManagerCreator
                    .createSharedEntityManager(factory);
        }

        @Bean
        PlatformTransactionManager transactionManager(
                EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
    }
}