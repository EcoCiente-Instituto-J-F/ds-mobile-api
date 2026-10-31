package br.com.ecociente.mobile.dataprovider.gateway;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.educacao.Quiz;
import br.com.ecociente.mobile.core.gateway.QuizGateway;
import br.com.ecociente.mobile.dataprovider.entity.AlternativaQuizEntity;
import br.com.ecociente.mobile.dataprovider.entity.PerguntaQuizEntity;
import br.com.ecociente.mobile.dataprovider.entity.QuizEntity;
import br.com.ecociente.mobile.dataprovider.mapper.QuizEntityMapper;
import br.com.ecociente.mobile.dataprovider.repository.AlternativaQuizRepository;
import br.com.ecociente.mobile.dataprovider.repository.PerguntaQuizRepository;
import br.com.ecociente.mobile.dataprovider.repository.QuizRepository;

@Component
public class QuizGatewayImpl implements QuizGateway {

    private final QuizRepository quizRepository;
    private final PerguntaQuizRepository perguntaRepository;
    private final AlternativaQuizRepository alternativaRepository;
    private final QuizEntityMapper mapper;

    public QuizGatewayImpl(
            QuizRepository quizRepository,
            PerguntaQuizRepository perguntaRepository,
            AlternativaQuizRepository alternativaRepository,
            QuizEntityMapper mapper) {
        this.quizRepository = quizRepository;
        this.perguntaRepository = perguntaRepository;
        this.alternativaRepository = alternativaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Quiz> buscarQuizDisponivelPorAulaId(Integer aulaId) {
        return quizRepository.buscarDisponivelPorAulaId(aulaId)
                .map(this::montarQuiz);
    }

    private Quiz montarQuiz(QuizEntity entity) {
        var perguntas = perguntaRepository.findByQuizIdOrderByOrdemAscIdAsc(
                entity.getId());

        List<Integer> perguntaIds = perguntas.stream()
                .map(PerguntaQuizEntity::getId)
                .toList();

        List<AlternativaQuizEntity> alternativas = perguntaIds.isEmpty()
                ? List.of()
                : alternativaRepository
                        .findByPerguntaIdInOrderByIdAsc(perguntaIds);

        return mapper.toDomain(entity, perguntas, alternativas);
    }

    @Override
    public Optional<Quiz> buscarQuizDisponivelPorId(Integer quizId) {
        return quizRepository.buscarDisponivelPorId(quizId)
                .map(this::montarQuiz);
    }
}