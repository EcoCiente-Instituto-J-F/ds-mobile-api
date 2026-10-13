package br.com.ecociente.mobile.core.domain;

public record Aula(
    Integer id,
    Integer cursoId,
    String titulo,
    String conteudo,
    Integer ordem
) {
}