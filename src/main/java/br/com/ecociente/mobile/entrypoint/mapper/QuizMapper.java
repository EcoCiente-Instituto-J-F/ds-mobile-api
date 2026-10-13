package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Quiz;
import br.com.ecociente.mobile.entrypoint.dto.response.QuizResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.QuizResponseDto.AlternativaResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.QuizResponseDto.PerguntaResponseDto;

@Component
public class QuizMapper {

    public QuizResponseDto toResponse(Quiz quiz) {
        return new QuizResponseDto(
            quiz.id(),
            quiz.cursoId(),
            quiz.aulaId(),
            quiz.titulo(),
            quiz.notaMinimaAprovacao().movePointLeft(1),
            quiz.pontosRecompensa(),
            quiz.perguntas().stream()
                .map(this::toResponse)
                .toList()
        );
    }

    private PerguntaResponseDto toResponse(Quiz.Pergunta pergunta) {
        return new PerguntaResponseDto(
            pergunta.id(),
            pergunta.enunciado(),
            pergunta.ordem(),
            pergunta.alternativas().stream()
                .map(alternativa -> new AlternativaResponseDto(
                    alternativa.id(),
                    alternativa.texto()
                ))
                .toList()
        );
    }
}