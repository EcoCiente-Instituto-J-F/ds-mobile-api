package br.com.ecociente.mobile.core.gateway.pontuacao;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.pontuacao.Quiz;

public interface QuizGateway {

  Optional<Quiz> buscarPorId(Integer quizId);
}