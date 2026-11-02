package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.ecociente.mobile.core.exception.senha.RegraNegocioException;
import br.com.ecociente.mobile.core.exception.senha.SincronizacaoSenhaException;
import br.com.ecociente.mobile.core.gateway.senha.EmailGateway;
import br.com.ecociente.mobile.core.gateway.senha.FirebaseSenhaGateway;
import br.com.ecociente.mobile.core.gateway.senha.SenhaEncoderGateway;
import br.com.ecociente.mobile.core.service.senha.SenhaService;
import br.com.ecociente.mobile.dataprovider.gateway.senha.RecuperarSenhaGatewayImpl;
import br.com.ecociente.mobile.dataprovider.gateway.senha.UsuarioSenhaGatewayImpl;
import br.com.ecociente.mobile.dataprovider.mapper.senha.RecuperarSenhaMapper;
import br.com.ecociente.mobile.dataprovider.mapper.senha.UsuarioSenhaPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.EsqueceuSenhaRepositoy;
import br.com.ecociente.mobile.dataprovider.repository.UsuarioRepository;

@Tag("integracao")
@Testcontainers
@SpringJUnitConfig(SenhaPersistenciaIntegrationTest.Config.class)
class SenhaPersistenciaIntegrationTest {

    private static final int USUARIO_ID = 20;
    private static final String EMAIL = "ana@teste.com";
    private static final String SENHA_ATUAL = "SenhaAtual@2026";
    private static final String NOVA_SENHA = "NovaSenha@2026";
    private static final String TOKEN = "0123";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SenhaService service;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private FirebaseSenhaGateway firebase;

    @BeforeEach
    void preparar() throws Exception {
        reset(firebase);

        // Somente o PostgreSQL isolado criado pelo Testcontainers.
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {

            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");
            statement.execute(recurso("sql/ecociente-schema.sql"));
            statement.execute("SET search_path TO public");
            statement.execute("SET search_path TO public");
            statement.execute(recurso("sql/senha-dados-test.sql"));
        }

        jdbc.update("""
                UPDATE tb_usuarios
                SET senha_hash = ?
                WHERE id_usuario = ?
                """, encoder.encode(SENHA_ATUAL), USUARIO_ID);

        jdbc.update("""
                INSERT INTO tb_recuperacoes_senha (
                    usuario_id, token_hash, expira_em
                )
                VALUES (?, ?, ?)
                """,
                USUARIO_ID,
                encoder.encode(TOKEN),
                OffsetDateTime.now().plusHours(1));
    }

    @Test
    void deveReverterRedefinicaoQuandoFirebaseFalha() {
        String hashAnterior = hashAtual();

        simularFalhaFirebase();

        assertThrows(
                SincronizacaoSenhaException.class,
                () -> service.redefinirSenha(
                        EMAIL, TOKEN, NOVA_SENHA, NOVA_SENHA));

        assertEquals(hashAnterior, hashAtual());
        assertEquals(1L, contarCodigosDisponiveis());

        verify(firebase).atualizarSenha(EMAIL, NOVA_SENHA);
    }

    @Test
    void devePersistirRedefinicaoEImpedirSegundoUso() {
        service.redefinirSenha(
                EMAIL, TOKEN, NOVA_SENHA, NOVA_SENHA);

        assertTrue(encoder.matches(NOVA_SENHA, hashAtual()));
        assertFalse(encoder.matches(SENHA_ATUAL, hashAtual()));

        assertEquals(0L, contarCodigosDisponiveis());

        assertEquals(
                Long.valueOf(1),
                jdbc.queryForObject("""
                        SELECT COUNT(*)
                        FROM tb_recuperacoes_senha
                        WHERE usuario_id = ?
                          AND utilizado_em IS NOT NULL
                        """, Long.class, USUARIO_ID));

        assertThrows(
                RegraNegocioException.class,
                () -> service.redefinirSenha(
                        EMAIL, TOKEN, NOVA_SENHA, NOVA_SENHA));

        verify(firebase, times(1))
                .atualizarSenha(EMAIL, NOVA_SENHA);
    }

    @Test
    void deveReverterAlteracaoQuandoFirebaseFalha() {
        String hashAnterior = hashAtual();

        simularFalhaFirebase();

        assertThrows(
                SincronizacaoSenhaException.class,
                () -> service.alterarSenha(
                        USUARIO_ID,
                        SENHA_ATUAL,
                        NOVA_SENHA,
                        NOVA_SENHA));

        assertEquals(hashAnterior, hashAtual());
        assertEquals(1L, contarCodigosDisponiveis());

        verify(firebase).atualizarSenha(EMAIL, NOVA_SENHA);
    }

    @Test
    void devePermitirApenasUmaRedefinicaoSimultanea() throws Exception {
        var prontos = new java.util.concurrent.CountDownLatch(2);
        var iniciar = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        java.util.concurrent.Callable<Boolean> tentativa = () -> {
            prontos.countDown();

            if (!iniciar.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tempo de espera excedido");
            }

            try {
                service.redefinirSenha(
                        EMAIL, TOKEN, NOVA_SENHA, NOVA_SENHA);
                return true;
            } catch (RegraNegocioException exception) {
                return false;
            }
        };

        try {
            var primeira = executor.submit(tentativa);
            var segunda = executor.submit(tentativa);

            assertTrue(
                    prontos.await(10, java.util.concurrent.TimeUnit.SECONDS),
                    "As duas tarefas devem estar prontas");

            iniciar.countDown();

            boolean resultadoUm = primeira.get(
                    30, java.util.concurrent.TimeUnit.SECONDS);

            boolean resultadoDois = segunda.get(
                    30, java.util.concurrent.TimeUnit.SECONDS);

            assertNotEquals(
                    resultadoUm,
                    resultadoDois,
                    "Somente uma redefinição deve ser aceita");

            assertTrue(encoder.matches(NOVA_SENHA, hashAtual()));
            assertEquals(0L, contarCodigosDisponiveis());

            verify(firebase, times(1))
                    .atualizarSenha(EMAIL, NOVA_SENHA);

        } finally {
            iniciar.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            10, java.util.concurrent.TimeUnit.SECONDS),
                    "As tarefas devem terminar");
        }
    }

    private void simularFalhaFirebase() {
        doThrow(new SincronizacaoSenhaException(
                "Firebase indisponível",
                new IllegalStateException("Falha simulada")))
                .when(firebase)
                .atualizarSenha(EMAIL, NOVA_SENHA);
    }

    private String hashAtual() {
        return jdbc.queryForObject("""
                SELECT senha_hash
                FROM tb_usuarios
                WHERE id_usuario = ?
                """, String.class, USUARIO_ID);
    }

    private long contarCodigosDisponiveis() {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM tb_recuperacoes_senha
                WHERE usuario_id = ?
                  AND utilizado_em IS NULL
                  AND expira_em > CURRENT_TIMESTAMP
                """, Long.class, USUARIO_ID);
    }

    private static String recurso(String caminho) throws Exception {
        try (var stream =
                     new ClassPathResource(caminho).getInputStream()) {
            return new String(
                    stream.readAllBytes(),
                    StandardCharsets.UTF_8);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import({
            SenhaService.class,
            UsuarioSenhaGatewayImpl.class,
            RecuperarSenhaGatewayImpl.class,
            UsuarioSenhaPersistenceMapper.class,
            RecuperarSenhaMapper.class
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
        LocalContainerEntityManagerFactoryBean entityManagerFactory(
                DataSource dataSource) {

            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

            factory.setManagedTypes(PersistenceManagedTypes.of(
                    "br.com.ecociente.mobile.dataprovider.entity.UsuarioEntity",
                    "br.com.ecociente.mobile.dataprovider.entity.TipoUsuarioEntity",
                    "br.com.ecociente.mobile.dataprovider.entity.EnderecoEntity",
                    "br.com.ecociente.mobile.dataprovider.entity.EsqueceuSenhaEntity"));

            factory.setJpaPropertyMap(
                    java.util.Map.of("hibernate.hbm2ddl.auto", "none"));

            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(
                EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        UsuarioRepository usuarioRepository(
                EntityManagerFactory factory) {
            return new JpaRepositoryFactory(
                    SharedEntityManagerCreator.createSharedEntityManager(factory))
                    .getRepository(UsuarioRepository.class);
        }

        @Bean
        EsqueceuSenhaRepositoy esqueceuSenhaRepository(
                EntityManagerFactory factory) {
            return new JpaRepositoryFactory(
                    SharedEntityManagerCreator.createSharedEntityManager(factory))
                    .getRepository(EsqueceuSenhaRepositoy.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        SenhaEncoderGateway senhaEncoderGateway(PasswordEncoder encoder) {
            return new SenhaEncoderGateway() {

                @Override
                public String gerarHash(String valor) {
                    return encoder.encode(valor);
                }

                @Override
                public boolean corresponde(String valor, String hash) {
                    return encoder.matches(valor, hash);
                }
            };
        }

        @Bean
        FirebaseSenhaGateway firebaseSenhaGateway() {
            return mock(FirebaseSenhaGateway.class);
        }

        @Bean
        EmailGateway emailGateway() {
            return mock(EmailGateway.class);
        }
    }
}