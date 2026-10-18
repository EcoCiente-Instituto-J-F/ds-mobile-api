package br.com.ecociente.mobile.core.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.AlternativaCorrecaoQuiz;
import br.com.ecociente.mobile.core.domain.ContextoCondominialQuiz;
import br.com.ecociente.mobile.core.domain.RespostaQuiz;
import br.com.ecociente.mobile.core.domain.RespostaQuizRegistrada;
import br.com.ecociente.mobile.core.domain.ResultadoTentativaQuiz;
import br.com.ecociente.mobile.core.domain.TentativaQuizEstado;
import br.com.ecociente.mobile.core.exception.FluxoQuizException;
import br.com.ecociente.mobile.core.exception.FluxoQuizException.Tipo;
import br.com.ecociente.mobile.core.gateway.QuizGateway;
import br.com.ecociente.mobile.core.gateway.TentativaQuizGateway;

@Service
public class FinalizarTentativaQuizService {

  private final QuizGateway quizGateway;
  private final TentativaQuizGateway tentativaGateway;

  public FinalizarTentativaQuizService(
      QuizGateway quizGateway,
      TentativaQuizGateway tentativaGateway) {
    this.quizGateway = quizGateway;
    this.tentativaGateway = tentativaGateway;
  }

  @Transactional
  public ResultadoTentativaQuiz executar(
      Integer usuarioId,
      Integer tentativaId,
      List<RespostaQuiz> respostas) {
    String tipoUsuario = validarUsuario(usuarioId);

    if (tentativaId == null || tentativaId <= 0) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "TENTATIVA_ID_INVALIDO",
          "Informe uma tentativa válida");
    }

    Map<Integer, Integer> enviadas = indexar(respostas);

    var tentativa = tentativaGateway
        .buscarComBloqueio(tentativaId)
        .orElseThrow(() -> erro(
            Tipo.NAO_ENCONTRADO,
            "TENTATIVA_NAO_ENCONTRADA",
            "Tentativa não encontrada"));

    if (!usuarioId.equals(tentativa.usuarioId())) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "TENTATIVA_NAO_PERTENCE_AO_USUARIO",
          "Você não pode finalizar esta tentativa");
    }

    if (tentativa.concluidoEm() != null) {
      return recuperarResultado(tentativa, enviadas);
    }

    var quiz = quizGateway
        .buscarQuizDisponivelPorId(tentativa.quizId())
        .orElseThrow(() -> erro(
            Tipo.NAO_ENCONTRADO,
            "QUIZ_INDISPONIVEL",
            "Quiz não encontrado ou indisponível"));

    var porPergunta = tentativaGateway
        .buscarGabarito(tentativa.quizId())
        .stream()
        .collect(Collectors.groupingBy(
            AlternativaCorrecaoQuiz::perguntaId));

    if (porPergunta.isEmpty()) {
      throw erro(
          Tipo.CONFLITO,
          "QUIZ_SEM_PERGUNTAS",
          "O quiz não possui perguntas");
    }

    if (!enviadas.keySet().equals(porPergunta.keySet())) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "RESPOSTAS_INCOMPLETAS",
          "Responda todas as perguntas deste quiz, sem incluir outras");
    }

    List<RespostaQuizRegistrada> corrigidas = new ArrayList<>();

    for (var entrada : porPergunta.entrySet()) {
      Integer perguntaId = entrada.getKey();
      var alternativas = entrada.getValue();

      long corretas = alternativas.stream()
          .filter(alternativa -> alternativa.alternativaId() != null
              && alternativa.correta())
          .count();

      if (corretas != 1) {
        throw erro(
            Tipo.CONFLITO,
            "GABARITO_INVALIDO",
            "Cada pergunta deve possuir exatamente uma alternativa correta");
      }

      Integer escolhidaId = enviadas.get(perguntaId);

      var escolhida = alternativas.stream()
          .filter(alternativa -> escolhidaId.equals(alternativa.alternativaId()))
          .findFirst()
          .orElseThrow(() -> erro(
              Tipo.DADOS_INVALIDOS,
              "ALTERNATIVA_INVALIDA",
              "Uma alternativa não pertence à pergunta informada"));

      corrigidas.add(new RespostaQuizRegistrada(
          perguntaId,
          escolhidaId,
          escolhida.correta()));
    }

    int acertos = Math.toIntExact(
        corrigidas.stream()
            .filter(RespostaQuizRegistrada::correta)
            .count());

    BigDecimal nota = BigDecimal.valueOf(acertos)
        .multiply(BigDecimal.valueOf(100))
        .divide(
            BigDecimal.valueOf(corrigidas.size()),
            2,
            RoundingMode.HALF_UP);

    boolean aprovado = nota.compareTo(quiz.notaMinimaAprovacao()) >= 0;

    var contexto = buscarContexto(usuarioId, tipoUsuario);

    tentativaGateway.salvarRespostas(
        tentativaId,
        corrigidas);

    var concluida = tentativaGateway.concluir(
        tentativaId,
        nota,
        aprovado,
        contexto.condominioId(),
        contexto.torreId());

    return montarResultado(concluida, corrigidas);
  }

  private ResultadoTentativaQuiz recuperarResultado(
      TentativaQuizEstado tentativa,
      Map<Integer, Integer> enviadas) {
    var registradas = tentativaGateway.buscarRespostas(
        tentativa.id());

    var anteriores = registradas.stream().collect(
        Collectors.toMap(
            RespostaQuizRegistrada::perguntaId,
            RespostaQuizRegistrada::alternativaId));

    if (!anteriores.equals(enviadas)) {
      throw erro(
          Tipo.CONFLITO,
          "TENTATIVA_JA_FINALIZADA",
          "Esta tentativa já foi finalizada com outras respostas");
    }

    if (registradas.isEmpty() || tentativa.nota() == null) {
      throw erro(
          Tipo.CONFLITO,
          "RESULTADO_INCONSISTENTE",
          "Não foi possível recuperar o resultado da tentativa");
    }

    return montarResultado(tentativa, registradas);
  }

  private ResultadoTentativaQuiz montarResultado(
      TentativaQuizEstado tentativa,
      List<RespostaQuizRegistrada> respostas) {
    int acertos = Math.toIntExact(
        respostas.stream()
            .filter(RespostaQuizRegistrada::correta)
            .count());

    return new ResultadoTentativaQuiz(
        tentativa.id(),
        tentativa.quizId(),
        tentativa.nota(),
        respostas.size(),
        acertos,
        tentativa.aprovado(),
        tentativa.concluidoEm());
  }

  private Map<Integer, Integer> indexar(
      List<RespostaQuiz> respostas) {
    if (respostas == null || respostas.isEmpty()) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "RESPOSTAS_OBRIGATORIAS",
          "Informe as respostas");
    }

    Map<Integer, Integer> resultado = new HashMap<>();

    for (var resposta : respostas) {
      if (resposta == null
          || resposta.perguntaId() == null
          || resposta.perguntaId() <= 0
          || resposta.alternativaId() == null
          || resposta.alternativaId() <= 0) {
        throw erro(
            Tipo.DADOS_INVALIDOS,
            "RESPOSTA_INVALIDA",
            "Informe pergunta e alternativa válidas");
      }

      if (resultado.putIfAbsent(
          resposta.perguntaId(),
          resposta.alternativaId()) != null) {
        throw erro(
            Tipo.DADOS_INVALIDOS,
            "PERGUNTA_REPETIDA",
            "Envie apenas uma resposta para cada pergunta");
      }
    }

    return resultado;
  }

  private String validarUsuario(Integer usuarioId) {
    if (usuarioId == null || usuarioId <= 0) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }

    String tipo = tentativaGateway
        .buscarTipoUsuarioAtivo(usuarioId)
        .map(this::normalizar)
        .orElseThrow(() -> erro(
            Tipo.NAO_PERMITIDO,
            "USUARIO_INATIVO_OU_INEXISTENTE",
            "Usuário não encontrado ou inativo"));

    if (!tipo.equals("comum")
        && !tipo.equals("usuario comum")
        && !tipo.equals("morador")) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "QUIZ_PERFIL_NAO_PERMITIDO",
          "Este usuário não pode finalizar tentativas");
    }

    return tipo;
  }

  private ContextoCondominialQuiz buscarContexto(
      Integer usuarioId,
      String tipo) {
    if (!tipo.equals("morador")) {
      return new ContextoCondominialQuiz(null, null);
    }

    var contextos = tentativaGateway
        .buscarContextosAprovadosDoMorador(usuarioId);

    if (contextos.size() != 1) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "VINCULO_CONDOMINIAL_INVALIDO",
          "O morador precisa de um vínculo aprovado com seu condomínio");
    }

    return contextos.getFirst();
  }

  private String normalizar(String valor) {
    return Normalizer.normalize(valor, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replace('_', ' ')
        .trim()
        .toLowerCase(Locale.ROOT);
  }

  private FluxoQuizException erro(
      Tipo tipo,
      String codigo,
      String mensagem) {
    return new FluxoQuizException(tipo, codigo, mensagem);
  }
}