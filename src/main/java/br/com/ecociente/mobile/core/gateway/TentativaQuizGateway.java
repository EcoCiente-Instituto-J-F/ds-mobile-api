package br.com.ecociente.mobile.core.gateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.AlternativaCorrecaoQuiz;
import br.com.ecociente.mobile.core.domain.ContextoCondominialQuiz;
import br.com.ecociente.mobile.core.domain.ItemRevisaoQuiz;
import br.com.ecociente.mobile.core.domain.RegistroTentativaQuiz;
import br.com.ecociente.mobile.core.domain.RespostaQuizRegistrada;
import br.com.ecociente.mobile.core.domain.TentativaQuizEstado;

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
}