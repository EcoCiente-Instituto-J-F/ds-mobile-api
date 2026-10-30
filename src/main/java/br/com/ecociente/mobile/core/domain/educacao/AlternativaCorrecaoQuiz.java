package br.com.ecociente.mobile.core.domain.educacao;

public record AlternativaCorrecaoQuiz(
    Integer perguntaId,
    Integer alternativaId,
    boolean correta
) {
}