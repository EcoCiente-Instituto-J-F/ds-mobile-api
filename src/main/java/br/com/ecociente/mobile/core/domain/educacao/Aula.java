package br.com.ecociente.mobile.core.domain.educacao;

public record Aula(
    Integer id,
    Integer cursoId,
    String titulo,
    String conteudo,
    Integer ordem
) {
}