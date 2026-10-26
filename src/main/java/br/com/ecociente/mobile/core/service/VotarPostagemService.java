package br.com.ecociente.mobile.core.service;

import org.springframework.stereotype.Service;

import br.com.ecociente.mobile.core.domain.ResultadoVoto;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.PostagemException.Tipo;
import br.com.ecociente.mobile.core.gateway.ValidadorPostagemGateway;

/**
 * Encaminha o voto do morador ao validador de postagens. As regras do voto
 * (peso, autovoto, motivo da denúncia, voto repetido) ficam lá.
 */
@Service
public class VotarPostagemService {

  private final ValidadorPostagemGateway gateway;

  public VotarPostagemService(ValidadorPostagemGateway gateway) {
    this.gateway = gateway;
  }

  public ResultadoVoto executar(
      Integer usuarioId,
      Integer postagemId,
      String tipo,
      Integer motivoDenunciaId,
      String comentario) {
    if (usuarioId == null || usuarioId <= 0) {
      throw new PostagemException(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }

    if (postagemId == null || postagemId <= 0) {
      throw new PostagemException(
          Tipo.DADOS_INVALIDOS,
          "POSTAGEM_ID_INVALIDO",
          "Informe uma postagem válida");
    }

    return gateway.votar(postagemId, usuarioId, tipo, motivoDenunciaId, comentario);
  }
}
