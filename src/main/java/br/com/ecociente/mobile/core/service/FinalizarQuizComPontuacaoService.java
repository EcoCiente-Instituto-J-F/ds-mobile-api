package br.com.ecociente.mobile.core.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.RespostaQuiz;
import br.com.ecociente.mobile.core.domain.ResultadoFinalizacaoQuiz;
import br.com.ecociente.mobile.core.domain.ResultadoPontuacaoQuiz;
import br.com.ecociente.mobile.core.gateway.PontuacaoQuizGateway;

@Service
public class FinalizarQuizComPontuacaoService {

  private final FinalizarTentativaQuizService finalizarService;
  private final PontuacaoQuizGateway pontuacaoGateway;

  public FinalizarQuizComPontuacaoService(
      FinalizarTentativaQuizService finalizarService,
      PontuacaoQuizGateway pontuacaoGateway) {
    this.finalizarService = finalizarService;
    this.pontuacaoGateway = pontuacaoGateway;
  }

  @Transactional(propagation = Propagation.NEVER)
  public ResultadoFinalizacaoQuiz executar(
      Integer usuarioId,
      Integer tentativaId,
      List<RespostaQuiz> respostas) {
    var resultado = finalizarService.executar(
        usuarioId,
        tentativaId,
        respostas);

    ResultadoPontuacaoQuiz pontuacao = resultado.aprovado()
        ? pontuacaoGateway.consolidar(
            resultado.tentativaId(),
            usuarioId)
        : ResultadoPontuacaoQuiz.naoAplicavel();

    return new ResultadoFinalizacaoQuiz(
        resultado,
        pontuacao);
  }
}