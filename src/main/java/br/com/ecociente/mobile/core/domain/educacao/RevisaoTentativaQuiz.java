package br.com.ecociente.mobile.core.domain.educacao;

import java.util.List;

public record RevisaoTentativaQuiz(
    ResultadoTentativaQuiz resultado,
    List<ItemRevisaoQuiz> perguntas) {
}