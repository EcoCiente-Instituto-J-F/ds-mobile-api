package br.com.ecociente.mobile.dataprovider.gateway.educacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.educacao.ResultadoPontuacaoQuiz;
import br.com.ecociente.mobile.core.service.pontuacao.ConsolidarQuizInternoService;
import br.com.ecociente.mobile.core.service.pontuacao.SincronizacaoPontuacaoService;
import br.com.ecociente.mobile.core.gateway.educacao.PontuacaoQuizGateway;

@Component
public class PontuacaoQuizGatewayImpl implements PontuacaoQuizGateway {

  private static final Logger log = LoggerFactory.getLogger(PontuacaoQuizGatewayImpl.class);

  private final ConsolidarQuizInternoService consolidacao;
  private final SincronizacaoPontuacaoService sincronizacao;

  public PontuacaoQuizGatewayImpl(
      ConsolidarQuizInternoService consolidacao,
      SincronizacaoPontuacaoService sincronizacao) {
    this.consolidacao = consolidacao;
    this.sincronizacao = sincronizacao;
  }

  @Override
  public ResultadoPontuacaoQuiz consolidar(
      Integer tentativaId,
      Integer usuarioId) {

    final br.com.ecociente.mobile.core.domain.pontuacao.MovimentacaoPontos movimento;

    try {
      movimento = consolidacao.executar(tentativaId, usuarioId);
    } catch (org.springframework.dao.DataAccessException exception) {
      log.error(
          "Falha ao consolidar pontuação da tentativa {}",
          tentativaId,
          exception);

      return ResultadoPontuacaoQuiz.pendente();
    } catch (br.com.ecociente.mobile.core.exception.pontuacao.DomainException exception) {
      log.warn(
          "Consolidação recusada para tentativa {}",
          tentativaId,
          exception);

      return ResultadoPontuacaoQuiz.falha();
    }

    boolean sincronizado = Boolean.TRUE.equals(movimento.getRedisSincronizado());

    if (!sincronizado) {
      try {
        sincronizado = sincronizacao.sincronizar(movimento.getId());
      } catch (RuntimeException exception) {
        log.warn(
            "Pontos gravados; Redis pendente para movimentação {}",
            movimento.getId(),
            exception);
      }
    }

    return new ResultadoPontuacaoQuiz(
        ResultadoPontuacaoQuiz.Status.CONFIRMADO,
        movimento.getPontos(),
        movimento.getId(),
        sincronizado);
  }
}