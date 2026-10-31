package br.com.ecociente.mobile.core.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.ItemRevisaoQuiz;
import br.com.ecociente.mobile.core.domain.educacao.ResultadoTentativaQuiz;
import br.com.ecociente.mobile.core.domain.educacao.ResumoTentativaQuiz;
import br.com.ecociente.mobile.core.domain.educacao.RevisaoTentativaQuiz;
import br.com.ecociente.mobile.core.exception.educacao.FluxoQuizException;
import br.com.ecociente.mobile.core.exception.educacao.FluxoQuizException.Tipo;
import br.com.ecociente.mobile.core.gateway.TentativaQuizGateway;

@Service
public class ConsultarRevisaoQuizService {

  private static final int LIMITE_MAXIMO_HISTORICO = 50;

  private final TentativaQuizGateway gateway;

  public ConsultarRevisaoQuizService(
      TentativaQuizGateway gateway) {
    this.gateway = gateway;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public RevisaoTentativaQuiz executar(
      Integer usuarioId,
      Integer tentativaId) {
    validarUsuario(usuarioId);

    if (tentativaId == null || tentativaId <= 0) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "TENTATIVA_ID_INVALIDO",
          "Informe uma tentativa válida");
    }

    var tentativa = gateway.buscarPorId(tentativaId)
        .orElseThrow(() -> erro(
            Tipo.NAO_ENCONTRADO,
            "TENTATIVA_NAO_ENCONTRADA",
            "Tentativa não encontrada"));

    if (!usuarioId.equals(tentativa.usuarioId())) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "TENTATIVA_NAO_PERTENCE_AO_USUARIO",
          "Você não pode consultar esta tentativa");
    }

    if (tentativa.concluidoEm() == null) {
      throw erro(
          Tipo.CONFLITO,
          "TENTATIVA_NAO_CONCLUIDA",
          "Finalize a tentativa antes de consultar a revisão");
    }

    var respostas = gateway.buscarRespostas(tentativaId);
    var itens = gateway.buscarItensRevisao(tentativaId);

    if (tentativa.nota() == null
        || respostas.isEmpty()
        || itens.size() != respostas.size()) {
      throw erro(
          Tipo.CONFLITO,
          "REVISAO_INCONSISTENTE",
          "Não foi possível recuperar todas as respostas da tentativa");
    }

    int acertos = Math.toIntExact(
        itens.stream()
            .filter(ItemRevisaoQuiz::acertouNaTentativa)
            .count());

    var resultado = new ResultadoTentativaQuiz(
        tentativa.id(),
        tentativa.quizId(),
        tentativa.nota(),
        respostas.size(),
        acertos,
        tentativa.aprovado(),
        tentativa.concluidoEm());

    return new RevisaoTentativaQuiz(
        resultado,
        itens);
  }

  private FluxoQuizException erro(
      Tipo tipo,
      String codigo,
      String mensagem) {
    return new FluxoQuizException(
        tipo,
        codigo,
        mensagem);
  }

  /** Últimas tentativas concluídas do usuário autenticado, da mais recente para a mais antiga. */
  @Transactional(readOnly = true)
  public List<ResumoTentativaQuiz> historico(
      Integer usuarioId,
      int limite) {
    validarUsuario(usuarioId);

    if (limite < 1 || limite > LIMITE_MAXIMO_HISTORICO) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "LIMITE_INVALIDO",
          "O limite deve estar entre 1 e " + LIMITE_MAXIMO_HISTORICO);
    }

    return gateway.listarConcluidas(usuarioId, limite);
  }

  /** Ids distintos dos quizzes já concluídos, aprovado ou não. Não usa o limite do histórico. */
  @Transactional(readOnly = true)
  public List<Integer> quizzesConcluidos(
      Integer usuarioId) {
    validarUsuario(usuarioId);

    return gateway.listarQuizzesConcluidos(usuarioId);
  }

  private void validarUsuario(
      Integer usuarioId) {
    if (usuarioId == null
        || usuarioId <= 0
        || gateway.buscarTipoUsuarioAtivo(usuarioId).isEmpty()) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário não encontrado ou inativo");
    }
  }
}
