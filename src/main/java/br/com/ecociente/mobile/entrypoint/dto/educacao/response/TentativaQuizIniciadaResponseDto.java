package br.com.ecociente.mobile.entrypoint.dto.educacao.response;

import java.time.OffsetDateTime;

public record TentativaQuizIniciadaResponseDto(
    Integer tentativaId,
    OffsetDateTime iniciadoEm,
    QuizResponseDto quiz) {
}