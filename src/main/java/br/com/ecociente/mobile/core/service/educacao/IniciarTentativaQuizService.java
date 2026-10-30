package br.com.ecociente.mobile.core.service.educacao;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.educacao.TentativaQuizIniciada;
import br.com.ecociente.mobile.core.exception.educacao.FluxoQuizException;
import br.com.ecociente.mobile.core.exception.educacao.FluxoQuizException.Tipo;
import br.com.ecociente.mobile.core.gateway.educacao.QuizGateway;
import br.com.ecociente.mobile.core.gateway.educacao.TentativaQuizGateway;

@Service
public class IniciarTentativaQuizService {

  private final QuizGateway quizGateway;
  private final TentativaQuizGateway tentativaGateway;

  public IniciarTentativaQuizService(
      QuizGateway quizGateway,
      TentativaQuizGateway tentativaGateway) {
    this.quizGateway = quizGateway;
    this.tentativaGateway = tentativaGateway;
  }

  @Transactional
  public TentativaQuizIniciada executar(
      Integer usuarioId,
      Integer quizId) {
    if (usuarioId == null || usuarioId <= 0) {
      throw new FluxoQuizException(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }

    if (quizId == null || quizId <= 0) {
      throw new FluxoQuizException(
          Tipo.DADOS_INVALIDOS,
          "QUIZ_ID_INVALIDO",
          "Informe um ID de quiz válido");
    }

    String tipo = tentativaGateway
        .buscarTipoUsuarioAtivo(usuarioId)
        .map(this::normalizar)
        .orElseThrow(() -> new FluxoQuizException(
            Tipo.NAO_PERMITIDO,
            "USUARIO_INATIVO_OU_INEXISTENTE",
            "Usuário não encontrado ou inativo"));

    // Usuário comum/comercial e morador (residencial ou não); síndico, cooperativa e admin ficam de fora.
    if (!tipo.equals("comum")
        && !tipo.startsWith("usuario")
        && !tipo.startsWith("morador")) {
      throw new FluxoQuizException(
          Tipo.NAO_PERMITIDO,
          "QUIZ_PERFIL_NAO_PERMITIDO",
          "Somente usuário comum e morador podem iniciar tentativas");
    }

    var quiz = quizGateway.buscarQuizDisponivelPorId(quizId)
        .orElseThrow(() -> new FluxoQuizException(
            Tipo.NAO_ENCONTRADO,
            "QUIZ_NAO_ENCONTRADO",
            "Quiz não encontrado ou indisponível"));

    if (quiz.perguntas().isEmpty()
        || quiz.perguntas().stream().anyMatch(
            pergunta -> pergunta.alternativas().isEmpty())) {
      throw new FluxoQuizException(
          Tipo.CONFLITO,
          "QUIZ_INCOMPLETO",
          "O quiz não está pronto para receber tentativas");
    }

    var registro = tentativaGateway.criar(usuarioId, quizId);

    return new TentativaQuizIniciada(
        registro.id(),
        registro.iniciadoEm(),
        quiz);
  }

  private String normalizar(String valor) {
    return Normalizer.normalize(
        valor,
        Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replace('_', ' ')
        .trim()
        .toLowerCase(Locale.ROOT);
  }
}