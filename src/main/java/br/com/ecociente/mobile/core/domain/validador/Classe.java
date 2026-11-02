package br.com.ecociente.mobile.core.domain.validador;

import java.util.List;

@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Classe(String slug, String nome, List<String> prompts) {
}
