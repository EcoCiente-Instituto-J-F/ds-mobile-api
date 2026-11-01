package br.com.ecociente.mobile.config.swagger;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

class SwaggerConfigTest {

  @Test
  void deveDeclararOEsquemaBearerComJwt() {
    SecurityScheme esquema = SwaggerConfig.class.getAnnotation(SecurityScheme.class);

    assertNotNull(esquema);
    assertEquals("bearerAuth", esquema.name());
    assertEquals(SecuritySchemeType.HTTP, esquema.type());
    assertEquals("bearer", esquema.scheme());
    assertEquals("JWT", esquema.bearerFormat());
  }

  @Test
  void deveExigirOTokenEmTodasAsOperacoes() {
    OpenAPIDefinition definicao = SwaggerConfig.class.getAnnotation(OpenAPIDefinition.class);

    assertNotNull(definicao);
    assertEquals(1, definicao.security().length);
    assertEquals(SwaggerConfig.ESQUEMA_BEARER, definicao.security()[0].name());
  }

  @Test
  void oRequisitoUsaOMesmoNomeDoEsquema() {
    SecurityScheme esquema = SwaggerConfig.class.getAnnotation(SecurityScheme.class);
    OpenAPIDefinition definicao = SwaggerConfig.class.getAnnotation(OpenAPIDefinition.class);

    assertEquals(esquema.name(), definicao.security()[0].name());
  }

  @Test
  void deveReferenciarServidorAtualNaDocumentacao() {
    OpenAPIDefinition definicao = SwaggerConfig.class.getAnnotation(OpenAPIDefinition.class);

    assertEquals(1, definicao.servers().length);
    assertEquals("/", definicao.servers()[0].url());
  }
}
