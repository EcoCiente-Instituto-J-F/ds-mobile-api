package br.com.ecociente.mobile.config.validador;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.io.IOException;
import java.nio.file.Path;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ai.onnxruntime.OrtException;
import br.com.ecociente.mobile.dataprovider.validador.visao.ClassificadorClipGateway;
import br.com.ecociente.mobile.dataprovider.validador.visao.ModeloClip;

/** Carrega o CLIP uma vez na subida (como o serviço antigo): o pod só abre a porta com o modelo pronto. */
@ConditionalOnProperty(name = "app.validador.ativo", havingValue = "true", matchIfMissing = true)
@Configuration
public class VisaoConfig {

  @Bean(destroyMethod = "close")
  public ModeloClip modeloClip(PropriedadesValidador propriedades) throws OrtException, IOException {
    return new ModeloClip(Path.of(propriedades.clipDir()));
  }

  @Bean
  public ClassificadorClipGateway classificadorClipGateway(ModeloClip modelo) {
    return new ClassificadorClipGateway(modelo);
  }
}
