package br.com.ecociente.mobile.dataprovider.mapper.pontuacao;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.pontuacao.TentativaQuiz;
import br.com.ecociente.mobile.dataprovider.entity.TentativaQuizEntity;

@Component
public class PontuacaoTentativaQuizMapper {

    public TentativaQuiz toDomain(TentativaQuizEntity entity) {
        return TentativaQuiz.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .quizId(entity.getQuizId())
                .condominioId(entity.getCondominioId())
                .torreId(entity.getTorreId())
                .nota(entity.getNota())
                .aprovado(entity.getAprovado())
                .iniciadoEm(entity.getIniciadoEm())
                .concluidoEm(entity.getConcluidoEm())
                .build();
    }
}