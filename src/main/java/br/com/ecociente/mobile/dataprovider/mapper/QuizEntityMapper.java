package br.com.ecociente.mobile.dataprovider.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Quiz;
import br.com.ecociente.mobile.dataprovider.entity.AlternativaQuizEntity;
import br.com.ecociente.mobile.dataprovider.entity.PerguntaQuizEntity;
import br.com.ecociente.mobile.dataprovider.entity.QuizEntity;

@Component
public class QuizEntityMapper {

    public Quiz toDomain(
        QuizEntity entity,
        List<PerguntaQuizEntity> perguntas,
        List<AlternativaQuizEntity> alternativas
    ) {
        Map<Integer, List<AlternativaQuizEntity>> porPergunta =
            alternativas.stream()
                .collect(Collectors.groupingBy(
                    AlternativaQuizEntity::getPerguntaId
                ));

        List<Quiz.Pergunta> perguntasDomain = perguntas.stream()
            .map(pergunta -> new Quiz.Pergunta(
                pergunta.getId(),
                pergunta.getEnunciado(),
                pergunta.getOrdem(),
                porPergunta.getOrDefault(pergunta.getId(), List.of())
                    .stream()
                    .map(alternativa -> new Quiz.Alternativa(
                        alternativa.getId(),
                        alternativa.getTexto()
                    ))
                    .toList()
            ))
            .toList();

        return new Quiz(
            entity.getId(),
            entity.getCursoId(),
            entity.getAulaId(),
            entity.getTitulo(),
            entity.getNotaMinimaAprovacao(),
            entity.getPontosRecompensa(),
            perguntasDomain
        );
    }
}