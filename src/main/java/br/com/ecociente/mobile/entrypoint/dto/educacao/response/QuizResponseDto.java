package br.com.ecociente.mobile.entrypoint.dto.educacao.response;

import java.math.BigDecimal;
import java.util.List;

public record QuizResponseDto(
    Integer id,
    Integer cursoId,
    Integer aulaId,
    String titulo,
    BigDecimal notaMinimaAprovacao,
    Integer pontosRecompensa,
    List<PerguntaResponseDto> perguntas
) {

    public record PerguntaResponseDto(
        Integer id,
        String enunciado,
        Integer ordem,
        List<AlternativaResponseDto> alternativas
    ) {
    }

    public record AlternativaResponseDto(
        Integer id,
        String texto
    ) {
    }
}