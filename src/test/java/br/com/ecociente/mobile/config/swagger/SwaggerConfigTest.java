package br.com.ecociente.mobile.config.swagger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

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
  void deveUsarOServidorAtualComUrlRelativa() {
    OpenAPIDefinition definicao = SwaggerConfig.class.getAnnotation(OpenAPIDefinition.class);

    assertEquals(1, definicao.servers().length);
    assertEquals("/", definicao.servers()[0].url());
  }

  @Test
  void aVersaoDaDocumentacaoVemDoBuild() {
    Properties propriedades = new Properties();
    propriedades.setProperty("version", "9.9.9");
    OpenAPI openApi = new OpenAPI().info(new Info().version("versao-da-anotacao"));

    var fabrica = new StaticListableBeanFactory();
    fabrica.addBean("buildProperties", new BuildProperties(propriedades));

    new SwaggerConfig()
        .versaoDoBuild(fabrica.getBeanProvider(BuildProperties.class))
        .customise(openApi);

    assertEquals("9.9.9", openApi.getInfo().getVersion());
  }
}
