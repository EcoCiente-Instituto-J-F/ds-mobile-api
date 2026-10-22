package br.com.ecociente.mobile;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Sobe a aplicação inteira com um Postgres temporário, sem depender de DB_URL e das outras
 * variáveis de ambiente. O schema não é validado (ddl-auto=none): o teste confere só que os
 * beans, as rotas e a segurança sobem juntos.
 */
@SpringBootTest
@Testcontainers
class MobileApplicationTests {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void propriedades(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
		registry.add("app.security.jwt.secret", () -> "0123456789abcdef0123456789abcdef");
	}

	@Test
	void contextLoads() {
	}

}
