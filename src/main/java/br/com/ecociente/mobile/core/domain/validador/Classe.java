package br.com.ecociente.mobile.core.domain.validador;

import java.util.List;

/** Classe que o modelo de visão reconhece: uma categoria de resíduo ou o "não resíduo" (fundo). */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Classe(String slug, String nome, List<String> prompts) {
}
