package br.com.ecociente.mobile.core.domain.educacao;

import java.math.BigDecimal;
import java.util.List;

public record Quiz(
    Integer id,
    Integer cursoId,
    Integer aulaId,
    String titulo,
    BigDecimal notaMinimaAprovacao,
    Integer pontosRecompensa,
    List<Pergunta> perguntas
) {

    public record Pergunta(
        Integer id,
        String enunciado,
        Integer ordem,
        List<Alternativa> alternativas
    ) {
    }

    public record Alternativa(
        Integer id,
        String texto
    ) {
    }
}