package br.com.ecociente.mobile.core.gateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.ItemRevisaoQuiz;
import br.com.ecociente.mobile.core.domain.educacao.AlternativaCorrecaoQuiz;
import br.com.ecociente.mobile.core.domain.educacao.ContextoCondominialQuiz;
import br.com.ecociente.mobile.core.domain.educacao.RegistroTentativaQuiz;
import br.com.ecociente.mobile.core.domain.educacao.RespostaQuizRegistrada;
import br.com.ecociente.mobile.core.domain.educacao.ResumoTentativaQuiz;
import br.com.ecociente.mobile.core.domain.educacao.TentativaQuizEstado;

public interface TentativaQuizGateway {

  Optional<String> buscarTipoUsuarioAtivo(Integer usuarioId);

  RegistroTentativaQuiz criar(
      Integer usuarioId,
      Integer quizId);

  Optional<TentativaQuizEstado> buscarComBloqueio(
      Integer tentativaId);

  List<AlternativaCorrecaoQuiz> buscarGabarito(
      Integer quizId);

  List<RespostaQuizRegistrada> buscarRespostas(
      Integer tentativaId);

  List<ContextoCondominialQuiz> buscarContextosAprovadosDoMorador(
      Integer usuarioId);

  void salvarRespostas(
      Integer tentativaId,
      List<RespostaQuizRegistrada> respostas);

  TentativaQuizEstado concluir(
      Integer tentativaId,
      BigDecimal nota,
      boolean aprovado,
      Integer condominioId,
      Integer torreId);

  Optional<TentativaQuizEstado> buscarPorId(
      Integer tentativaId);

  List<ItemRevisaoQuiz> buscarItensRevisao(
      Integer tentativaId);

  List<ResumoTentativaQuiz> listarConcluidas(
      Integer usuarioId,
      int limite);

  List<Integer> listarQuizzesConcluidos(
      Integer usuarioId);
}
