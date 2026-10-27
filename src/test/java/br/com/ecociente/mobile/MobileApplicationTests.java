package br.com.ecociente.mobile;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifica a inicializacao completa da API com um PostgreSQL isolado.
 * As suites de integracao verificam os dados e contratos de negocio.
 */
@Testcontainers
@SpringBootTest(properties = {
    "app.security.jwt.secret=ecociente-teste-contexto-jwt-0123456789abcdef",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.open-in-view=false",
    "spring.sql.init.mode=never",
    "spring.flyway.enabled=false",
    "spring.liquibase.enabled=false"
})
class MobileApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mobile_contexto_test")
            .withUsername("teste")
            .withPassword("teste");

    @Test
    void contextLoads() {
    }
}
