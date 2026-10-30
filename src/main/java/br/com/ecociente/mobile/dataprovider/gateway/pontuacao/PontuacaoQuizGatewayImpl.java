package br.com.ecociente.mobile.dataprovider.gateway.pontuacao;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.pontuacao.Quiz;
import br.com.ecociente.mobile.core.gateway.pontuacao.QuizGateway;
import br.com.ecociente.mobile.dataprovider.mapper.pontuacao.PontuacaoQuizMapper;
import br.com.ecociente.mobile.dataprovider.repository.QuizRepository;
import lombok.RequiredArgsConstructor;

@Component("quizConsultaPontuacaoGateway")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PontuacaoQuizGatewayImpl implements QuizGateway {

    private final QuizRepository repository;
    private final PontuacaoQuizMapper mapper;

    @Override
    public Optional<Quiz> buscarPorId(Integer quizId) {
        return repository.findById(quizId)
                .map(mapper::toDomain);
    }
}