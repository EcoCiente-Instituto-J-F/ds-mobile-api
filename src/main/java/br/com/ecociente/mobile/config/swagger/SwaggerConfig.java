package br.com.ecociente.mobile.config.swagger;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Faz o botão Authorize do Swagger enviar o JWT da ds-autenticacao-api. O requisito de segurança
 * vale para todas as operações, porque quase todas as rotas desta API exigem o token.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "API Mobile",
        version = "1.0.0",
        description = """
            API consumida pelo aplicativo EcoCiente: perfil do usuário, cursos, aulas e quizzes,
            postagens de descarte, cooperativas, avaliação de coletas e dashboard do síndico.
            Para testar, faça o login na ds-autenticacao-api, clique em Authorize e cole o token.
            """,
        contact = @Contact(name = "Equipe EcoCiente")),
    servers = {
        @Server(
            url = "http://localhost:9654",
            description = "Ambiente local"),
        @Server(
            url = "https://ds-mobile-api.onrender.com",
            description = "Produção")
    },
    security = @SecurityRequirement(name = SwaggerConfig.ESQUEMA_BEARER))
@SecurityScheme(
    name = SwaggerConfig.ESQUEMA_BEARER,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class SwaggerConfig {

  public static final String ESQUEMA_BEARER = "bearerAuth";
}
