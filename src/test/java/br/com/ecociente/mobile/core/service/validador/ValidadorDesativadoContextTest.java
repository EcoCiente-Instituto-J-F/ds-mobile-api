package br.com.ecociente.mobile.core.service.validador;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ValidadorDesativadoContextTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            TriagemFotoService.class,
                            TriagemPostagemService.class);

    @Test
    void naoInstanciaTriagemComValidadorDesligado() {
        contextRunner
                .withPropertyValues("app.validador.ativo=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(TriagemFotoService.class);
                    assertThat(context).doesNotHaveBean(TriagemPostagemService.class);
                });
    }
}
