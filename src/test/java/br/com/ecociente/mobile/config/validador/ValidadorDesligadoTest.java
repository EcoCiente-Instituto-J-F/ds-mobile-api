package br.com.ecociente.mobile.config.validador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import br.com.ecociente.mobile.core.gateway.validador.PostagemValidacaoGateway;
import br.com.ecociente.mobile.core.service.validador.TriagemFotoService;
import br.com.ecociente.mobile.core.service.validador.TriagemPostagemService;
import br.com.ecociente.mobile.dataprovider.validador.foto.FotoHttpGateway;

/**
 * VALIDADOR_ATIVO=false precisa subir a aplicação sem o CLIP, sem HOSTS_PERMITIDOS e sem CLIP_DIR.
 * Lê o application.properties real, para pegar também o mapeamento da variável.
 */
class ValidadorDesligadoTest {

  private final ApplicationContextRunner contexto = new ApplicationContextRunner()
      .withInitializer(new ConfigDataApplicationContextInitializer())
      .withPropertyValues("spring.config.import=")
      .withBean(PostagemValidacaoGateway.class, () -> mock(PostagemValidacaoGateway.class))
      .withUserConfiguration(
          TriagemConfig.class,
          VisaoConfig.class,
          FotoHttpGateway.class,
          TriagemFotoService.class,
          TriagemPostagemService.class);

  @Test
  void validadorAtivoFalseNaoCriaATriagemESobeSemOModelo() {
    contexto
        .withPropertyValues("VALIDADOR_ATIVO=false")
        .run(app -> {
          assertThat(app).hasNotFailed();
          assertThat(app).doesNotHaveBean(TriagemFotoService.class);
          assertThat(app).doesNotHaveBean(TriagemPostagemService.class);
          assertThat(app).doesNotHaveBean(FotoHttpGateway.class);
        });
  }
}
