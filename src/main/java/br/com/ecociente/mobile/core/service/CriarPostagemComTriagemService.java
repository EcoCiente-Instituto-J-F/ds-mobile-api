package br.com.ecociente.mobile.core.service;

import org.springframework.beans.factory.ObjectProvider;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.Postagem;
import br.com.ecociente.mobile.core.gateway.PostagemGateway;
import br.com.ecociente.mobile.core.service.validador.TriagemPostagemService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CriarPostagemComTriagemService {

  private static final Logger log = LoggerFactory.getLogger(CriarPostagemComTriagemService.class);

  private final CriarPostagemService criarService;
  private final ObjectProvider<TriagemPostagemService> triagemService;
  private final PostagemGateway postagemGateway;

  @Transactional(propagation = Propagation.NEVER)
  public Postagem executar(
      Integer usuarioId,
      Integer condominioId,
      Integer categoriaId,
      String urlFoto,
      String hashFoto,
      OffsetDateTime capturadaEm) {

    var postagem = criarService.executar(
        usuarioId,
        condominioId,
        categoriaId,
        urlFoto,
        hashFoto,
        capturadaEm);

    var triagem = triagemService.getIfAvailable();

    if (triagem == null) {
      log.info(
          "Validador desligado (VALIDADOR_ATIVO=false): postagem {} segue para a votação dos moradores",
          postagem.id());
      return postagem;
    }

    try {
      var veredito = triagem.triar(postagem.id());

      log.info(
          "Triagem da postagem {} concluída: {}",
          postagem.id(),
          veredito.status());

    } catch (RuntimeException exception) {
      log.error(
          "Postagem {} criada; triagem não concluída",
          postagem.id(),
          exception);
    }

    return postagemGateway.buscarPorId(postagem.id())
        .orElse(postagem);
  }
}