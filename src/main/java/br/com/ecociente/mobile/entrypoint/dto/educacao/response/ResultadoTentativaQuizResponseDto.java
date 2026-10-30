package br.com.ecociente.mobile.entrypoint.dto.educacao.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ResultadoTentativaQuizResponseDto(
        Integer tentativaId,
        Integer quizId,
        BigDecimal nota,
        int totalPerguntas,
        int acertos,
        boolean aprovado,
        OffsetDateTime concluidoEm,
        PontuacaoResponseDto pontuacao) {

    public record PontuacaoResponseDto(
            String status,
            Integer pontos,
            Long movimentacaoId,
            Boolean rankingSincronizado) {
    }
}