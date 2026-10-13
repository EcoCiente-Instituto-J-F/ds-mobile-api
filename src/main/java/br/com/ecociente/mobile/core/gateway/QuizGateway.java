package br.com.ecociente.mobile.core.gateway;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.Quiz;

public interface QuizGateway {

    Optional<Quiz> buscarQuizDisponivelPorAulaId(Integer aulaId);
}