package br.com.ecociente.mobile.core.gateway;

import br.com.ecociente.mobile.core.domain.ResultadoPontuacaoQuiz;

public interface PontuacaoQuizGateway {

  ResultadoPontuacaoQuiz consolidar(
      Integer tentativaId,
      Integer usuarioId);
}