package br.com.ecociente.mobile.dataprovider.gateway;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

import br.com.ecociente.mobile.core.domain.ResultadoVoto;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.PostagemException.Tipo;
import br.com.ecociente.mobile.core.exception.validador.NotFoundException;
import br.com.ecociente.mobile.core.exception.validador.PermissaoNegadaException;
import br.com.ecociente.mobile.core.gateway.ValidadorPostagemGateway;
import br.com.ecociente.mobile.core.service.validador.ValidadorPostagemValidacaoService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ValidadorPostagemGatewayImpl
    implements ValidadorPostagemGateway {

  private final ValidadorPostagemValidacaoService service;

  @Override
  public ResultadoVoto votar(
      Integer postagemId,
      Integer usuarioId,
      String tipo,
      Integer motivoDenunciaId,
      String comentario) {

    try {
      var resultado = service.votar(
          postagemId,
          usuarioId,
          tipo,
          motivoDenunciaId,
          comentario);

      return new ResultadoVoto(
          resultado.saldoConfianca(),
          resultado.pontuacaoAtiva());

    } catch (NotFoundException exception) {
      throw erro(
          Tipo.NAO_ENCONTRADO,
          "POSTAGEM_NAO_ENCONTRADA",
          exception.getMessage());

    } catch (PermissaoNegadaException exception) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "VOTO_NAO_PERMITIDO",
          exception.getMessage());

    } catch (CannotCreateTransactionException exception) {
      throw indisponivel();

    } catch (DataAccessException exception) {
      throw traduzir(exception);
    }
  }

  private PostagemException traduzir(DataAccessException exception) {
    SQLException sql = causaSql(exception);

    if (sql == null || sql.getSQLState() == null) {
      return indisponivel();
    }

    return switch (sql.getSQLState()) {
      case "23505" -> erro(
          Tipo.CONFLITO,
          "VOTO_JA_REGISTRADO",
          "Você já votou nesta postagem");

      case "23503" -> erro(
          Tipo.DADOS_INVALIDOS,
          "REFERENCIA_INVALIDA",
          "Referência inválida para o voto");

      case "P0001" -> traduzirRegra(sql.getMessage());

      default -> indisponivel();
    };
  }

  private PostagemException traduzirRegra(String mensagem) {
    String detalhe = mensagem == null ? "" : mensagem;

    if (detalhe.contains("Janela comunitaria da postagem")
        || detalhe.contains("nao aceita novos votos")) {
      return erro(
          Tipo.CONFLITO,
          "VOTACAO_ENCERRADA",
          "A votação desta postagem já foi encerrada");
    }

    if (detalhe.contains("motivo_denuncia_id e obrigatorio")) {
      return erro(
          Tipo.DADOS_INVALIDOS,
          "MOTIVO_DENUNCIA_OBRIGATORIO",
          "Informe o motivo da denúncia");
    }

    if (detalhe.contains("motivo_denuncia_id deve ser nulo")) {
      return erro(
          Tipo.DADOS_INVALIDOS,
          "MOTIVO_DENUNCIA_INDEVIDO",
          "Voto de aprovação não leva motivo de denúncia");
    }

    if (detalhe.contains("nao possui vinculo aprovado")) {
      return erro(
          Tipo.NAO_PERMITIDO,
          "SEM_VINCULO_APROVADO",
          "Você não tem vínculo aprovado com este condomínio");
    }

    return erro(
        Tipo.DADOS_INVALIDOS,
        "VOTO_INVALIDO",
        "Voto inválido");
  }

  private SQLException causaSql(Throwable exception) {
    for (Throwable atual = exception; atual != null; atual = atual.getCause()) {
      if (atual instanceof SQLException sql) {
        return sql;
      }
    }

    return null;
  }

  private PostagemException indisponivel() {
    return erro(
        Tipo.INDISPONIVEL,
        "VALIDADOR_INDISPONIVEL",
        "Não foi possível registrar o voto agora. Tente novamente.");
  }

  private PostagemException erro(
      Tipo tipo,
      String codigo,
      String mensagem) {
    return new PostagemException(tipo, codigo, mensagem);
  }
}