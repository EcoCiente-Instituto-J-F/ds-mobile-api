package br.com.ecociente.mobile.core.service.validador;

import java.awt.image.BufferedImage;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.ecociente.mobile.core.domain.validador.ClasseProbabilidade;
import br.com.ecociente.mobile.core.domain.validador.FotoCarregada;
import br.com.ecociente.mobile.core.domain.validador.ParametrosTriagem;
import br.com.ecociente.mobile.core.domain.validador.VeredictoFoto;
import br.com.ecociente.mobile.core.exception.validador.FotoException;
import br.com.ecociente.mobile.core.imagem.validador.DHash;
import br.com.ecociente.mobile.core.gateway.validador.FotoGateway;
import br.com.ecociente.mobile.core.gateway.validador.ClassificadorGateway;

@Service
public class TriagemFotoService {

  private final FotoGateway fotos;
  private final ClassificadorGateway classificador;
  private final ParametrosTriagem parametros;

  public TriagemFotoService(FotoGateway fotos, ClassificadorGateway classificador, ParametrosTriagem parametros) {
    this.fotos = fotos;
    this.classificador = classificador;
    this.parametros = parametros;
  }

  public VeredictoFoto analisar(String urlFoto, String categoria) {
    FotoCarregada foto;
    try {
      foto = fotos.baixar(urlFoto);
    } catch (FotoException erro) {
      return VeredictoFoto.rejeitada(categoria, erro.getMessage(), erro.getStatus());
    }

    BufferedImage imagem = foto.imagem();
    List<ClasseProbabilidade> ranking = classificador.ranking(imagem);
    VeredictoFoto veredito = RegraPertinencia.decidir(
        categoria, ranking, parametros.limiarPara(categoria), parametros.limiarRevisao());
    List<String> sinais = foto.semExif() ? List.of("sem_exif") : List.of();
    return veredito.comSinais(DHash.calcular(imagem), sinais);
  }
}
