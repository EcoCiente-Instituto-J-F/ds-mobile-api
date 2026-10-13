package br.com.ecociente.mobile.core.domain;

public record Curso(
    Integer id,
    String titulo,
    String descricao,
    boolean estaAtivo
) {
}