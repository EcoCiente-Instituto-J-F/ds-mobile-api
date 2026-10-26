package br.com.ecociente.mobile.core.domain;

import java.time.OffsetDateTime;

public record Conversa(
    Integer id,
    Integer condominioId,
    Integer cooperativaId,
    String nomeCondominio,
    String nomeCooperativa,
    OffsetDateTime criadaEm,
    OffsetDateTime ultimaMensagemEm,
    boolean ativa,
    long naoLidas
) {
}