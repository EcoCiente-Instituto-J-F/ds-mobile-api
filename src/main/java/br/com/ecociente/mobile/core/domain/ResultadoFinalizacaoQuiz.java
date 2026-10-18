package br.com.ecociente.mobile.core.domain;

public record ResultadoFinalizacaoQuiz(
    ResultadoTentativaQuiz tentativa,
    ResultadoPontuacaoQuiz pontuacao) {
}