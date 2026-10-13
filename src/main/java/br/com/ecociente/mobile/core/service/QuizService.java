package br.com.ecociente.mobile.core.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.Quiz;
import br.com.ecociente.mobile.core.gateway.QuizGateway;

@Service
public class QuizService {

    private final QuizGateway gateway;

    public QuizService(QuizGateway gateway) {
        this.gateway = gateway;
    }

    @Transactional(readOnly = true)
    public Optional<Quiz> buscarQuizDaAula(Integer aulaId) {
        return gateway.buscarQuizDisponivelPorAulaId(aulaId);
    }
}