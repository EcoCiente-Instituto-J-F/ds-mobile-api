package br.com.ecociente.mobile.core.domain;

import java.util.List;

public record RevisaoTentativaQuiz(
    ResultadoTentativaQuiz resultado,
    List<ItemRevisaoQuiz> perguntas) {
}