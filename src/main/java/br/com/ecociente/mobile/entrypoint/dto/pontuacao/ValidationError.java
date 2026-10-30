package br.com.ecociente.mobile.entrypoint.dto.pontuacao;

public record ValidationError(
    String field,
    String message) {
}