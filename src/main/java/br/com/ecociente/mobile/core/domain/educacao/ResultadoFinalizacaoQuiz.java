package br.com.ecociente.mobile.core.domain.educacao;

public record ResultadoFinalizacaoQuiz(
    ResultadoTentativaQuiz tentativa,
    ResultadoPontuacaoQuiz pontuacao) {
}