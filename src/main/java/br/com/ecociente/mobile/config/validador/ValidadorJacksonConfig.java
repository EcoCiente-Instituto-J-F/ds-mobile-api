package br.com.ecociente.mobile.config.validador;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.ecociente.mobile.core.domain.validador.Alternativa;
import br.com.ecociente.mobile.core.domain.validador.Categorias;
import br.com.ecociente.mobile.core.domain.validador.Classe;
import br.com.ecociente.mobile.core.domain.validador.ClasseProbabilidade;
import br.com.ecociente.mobile.core.domain.validador.DadosTriagem;
import br.com.ecociente.mobile.core.domain.validador.FotoCarregada;
import br.com.ecociente.mobile.core.domain.validador.ParametrosTriagem;
import br.com.ecociente.mobile.core.domain.validador.ResultadoVoto;
import br.com.ecociente.mobile.core.domain.validador.StatusTriagem;
import br.com.ecociente.mobile.core.domain.validador.VeredictoFoto;
import br.com.ecociente.mobile.core.domain.validador.VinculoTrust;

@Configuration(proxyBeanMethods = false)
public class ValidadorJacksonConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer validadorJsonCustomizer() {
    return builder -> {
      Class<?>[] tipos = {
          Alternativa.class,
          Categorias.class,
          Classe.class,
          ClasseProbabilidade.class,
          DadosTriagem.class,
          FotoCarregada.class,
          ParametrosTriagem.class,
          ResultadoVoto.class,
          VeredictoFoto.class,
          VinculoTrust.class
      };

      for (Class<?> tipo : tipos) {
        builder.mixIn(tipo, SnakeCaseMixin.class);
      }

      builder.mixIn(StatusTriagem.class, StatusTriagemMixin.class);
    };
  }

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  private abstract static class SnakeCaseMixin {
  }

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  private abstract static class StatusTriagemMixin {

    @JsonValue
    public abstract String getValor();
  }
}