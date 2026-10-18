package br.com.ecociente.mobile.core.domain;

public record RespostaQuizRegistrada(
    Integer perguntaId,
    Integer alternativaId,
    boolean correta) {
}