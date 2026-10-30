package br.com.ecociente.mobile.core.domain.validador;

/** O que a triagem precisa da postagem: a URL da foto e o nome da categoria escolhida pelo morador. */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DadosTriagem(String urlFoto, String nomeCategoria) {
}
