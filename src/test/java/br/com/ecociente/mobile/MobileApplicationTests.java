package br.com.ecociente.mobile;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.google.firebase.auth.FirebaseAuth;

import br.com.ecociente.mobile.core.gateway.pontuacao.PostagemValidacaoGateway;
import br.com.ecociente.mobile.core.gateway.pontuacao.SincronizacaoPontuacaoGateway;
import br.com.ecociente.mobile.core.gateway.senha.FirebaseSenhaGateway;
import br.com.ecociente.mobile.core.service.CriarPostagemComTriagemService;
import br.com.ecociente.mobile.dataprovider.validador.visao.ClassificadorClipGateway;
import br.com.ecociente.mobile.dataprovider.validador.visao.ModeloClip;

@Testcontainers
@SpringBootTest(
    classes = MobileApplication.class,
    properties = {
        "spring.config.import=",
        "app.security.jwt.secret=ecociente-teste-contexto-jwt-0123456789abcdef",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false",

        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=teste",
        "spring.mail.password=teste",
        "app.mail.remetente=teste@ecociente.test",

        "app.firebase.credenciais={}",
        "app.senha.url-redefinicao=http://localhost/redefinir-senha",

        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.username=",
        "spring.data.redis.password=",

        "app.ranking.cycle-epoch=2026-01-01",
        "app.validador.hosts-permitidos=storage.teste",
        "app.validador.clip-dir=modelo-simulado",
        "management.health.mail.enabled=false"
    }
)
class MobileApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("mobile_contexto_test")
                    .withUsername("teste")
                    .withPassword("teste");

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ModeloClip modeloClip;

    @MockitoBean
    private ClassificadorClipGateway classificadorClipGateway;

    @MockitoBean
    private PostagemValidacaoGateway postagemValidacaoGateway;

    @MockitoBean
    private SincronizacaoPontuacaoGateway sincronizacaoPontuacaoGateway;

    @Test
    void contextLoads() {
        assertNotNull(context.getBean(FirebaseSenhaGateway.class));
        assertNotNull(context.getBean(CriarPostagemComTriagemService.class));
        assertNotNull(context.getBean("authenticationManager"));
    }
}