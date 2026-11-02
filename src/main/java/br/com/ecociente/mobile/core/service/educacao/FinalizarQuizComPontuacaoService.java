package br.com.ecociente.mobile.core.service.educacao;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.ecociente.mobile.core.service.notificacoes.PushService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.educacao.RespostaQuiz;
import br.com.ecociente.mobile.core.domain.educacao.ResultadoFinalizacaoQuiz;
import br.com.ecociente.mobile.core.domain.educacao.ResultadoPontuacaoQuiz;
import br.com.ecociente.mobile.core.gateway.educacao.PontuacaoQuizGateway;

@Service
public class FinalizarQuizComPontuacaoService {

  private final FinalizarTentativaQuizService finalizarService;
  private final PontuacaoQuizGateway pontuacaoGateway;
  @Autowired(required = false)
  private PushService push;

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

    if (resultado.aprovado() && push != null) {
      push.criar(usuarioId, "motivacional", "quiz_aprovado:" + tentativaId,
          "Quiz concluído!", "Seu aprendizado faz diferença.", "quiz", null);
    }
    return new ResultadoFinalizacaoQuiz(
        resultado,
        pontuacao);
  }
}