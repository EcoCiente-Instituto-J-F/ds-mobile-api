package br.com.ecociente.mobile.dataprovider.mapper.pontuacao;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.pontuacao.Quiz;
import br.com.ecociente.mobile.dataprovider.entity.QuizEntity;

@Component
public class PontuacaoQuizMapper {

    public Quiz toDomain(QuizEntity entity) {
        return Quiz.builder()
                .id(entity.getId())
                .tituloQuiz(entity.getTitulo())
                .pontosRecompensa(entity.getPontosRecompensa())
                .ativo(entity.isAtivo())
                .build();
    }
}