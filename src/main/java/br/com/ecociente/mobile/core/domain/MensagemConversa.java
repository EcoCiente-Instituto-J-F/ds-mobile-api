package br.com.ecociente.mobile.core.domain;

import java.time.OffsetDateTime;

public record MensagemConversa(
    Long id,
    Integer conversaId,
    Integer remetenteUsuarioId,
    String remetenteTipo,
    String conteudo,
    OffsetDateTime enviadaEm,
    OffsetDateTime lidaEm) {
}