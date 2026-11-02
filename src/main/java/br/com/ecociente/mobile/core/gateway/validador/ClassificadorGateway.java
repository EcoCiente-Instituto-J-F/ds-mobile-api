package br.com.ecociente.mobile.core.gateway.validador;

import java.awt.image.BufferedImage;
import java.util.List;
import br.com.ecociente.mobile.core.domain.validador.ClasseProbabilidade;

public interface ClassificadorGateway {

    List<ClasseProbabilidade> ranking(BufferedImage imagem);
}
