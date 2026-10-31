package br.com.ecociente.mobile.core.service.validador;

import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.ecociente.mobile.core.domain.validador.Categorias;
import br.com.ecociente.mobile.core.domain.validador.Classe;
import br.com.ecociente.mobile.core.domain.validador.DadosTriagem;
import br.com.ecociente.mobile.core.domain.validador.StatusTriagem;
import br.com.ecociente.mobile.core.domain.validador.VeredictoFoto;
import br.com.ecociente.mobile.core.exception.validador.CategoriaInvalidaException;
import br.com.ecociente.mobile.core.exception.validador.NotFoundException;
import br.com.ecociente.mobile.core.exception.validador.ServicoOcupadoException;
import br.com.ecociente.mobile.core.gateway.validador.PostagemValidacaoGateway;

@Service
public class TriagemPostagemService {

  private final PostagemValidacaoGateway postagens;
  private final TriagemFotoService triagemFoto;
  private final Semaphore vagas;

  public TriagemPostagemService(
      PostagemValidacaoGateway postagens,
      TriagemFotoService triagemFoto,
      @Value("${app.validador.max-pedidos-simultaneos}") int maxPedidosSimultaneos) {
    this.postagens = postagens;
    this.triagemFoto = triagemFoto;
    this.vagas = new Semaphore(maxPedidosSimultaneos);
  }

  public VeredictoFoto triar(int postagemId) {
    DadosTriagem dados = postagens.buscarDadosTriagem(postagemId)
        .orElseThrow(() -> new NotFoundException("Postagem não encontrada."));
    String categoria = categoriaDoCatalogo(dados.nomeCategoria());

    if (!vagas.tryAcquire()) {
      throw new ServicoOcupadoException();
    }
    VeredictoFoto veredito;
    try {
      veredito = triagemFoto.analisar(dados.urlFoto(), categoria);
    } finally {
      vagas.release();
    }

    if (veredito.status() == StatusTriagem.OK) {
      postagens.gravarTriagem(postagemId, veredito.pertinente(), veredito.confianca());
    }
    return veredito;
  }

  private static String categoriaDoCatalogo(String nomeCategoria) {
    return Categorias.porSlug(Categorias.normalizar(nomeCategoria))
        .map(Classe::slug)
        .orElseThrow(() -> new CategoriaInvalidaException(nomeCategoria));
  }
}
