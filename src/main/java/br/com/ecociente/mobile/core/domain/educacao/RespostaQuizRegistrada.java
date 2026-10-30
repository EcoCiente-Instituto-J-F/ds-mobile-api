package br.com.ecociente.mobile.core.domain.educacao;

public record RespostaQuizRegistrada(
    Integer perguntaId,
    Integer alternativaId,
    boolean correta) {
}