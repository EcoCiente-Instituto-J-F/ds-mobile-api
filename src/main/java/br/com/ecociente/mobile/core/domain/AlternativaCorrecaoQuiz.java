package br.com.ecociente.mobile.core.domain;

public record AlternativaCorrecaoQuiz(
    Integer perguntaId,
    Integer alternativaId,
    boolean correta
) {
}