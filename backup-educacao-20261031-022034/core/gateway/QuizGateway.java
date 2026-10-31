package br.com.ecociente.mobile.core.gateway;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.educacao.Quiz;

public interface QuizGateway {

    Optional<Quiz> buscarQuizDisponivelPorAulaId(Integer aulaId);
    Optional<Quiz> buscarQuizDisponivelPorId(Integer quizId);
}