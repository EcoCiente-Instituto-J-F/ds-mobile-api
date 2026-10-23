package br.com.ecociente.mobile.core.domain;

/**
 * O que a avaliação precisa saber do agendamento: quem é o síndico do condomínio
 * e se a coleta já foi realizada.
 */
public record AgendamentoAvaliavel(
    Integer id,
    Integer sindicoUsuarioId,
    boolean realizado
) {
}
