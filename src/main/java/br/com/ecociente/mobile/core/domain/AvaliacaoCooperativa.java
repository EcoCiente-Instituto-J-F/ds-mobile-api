package br.com.ecociente.mobile.core.domain;

import java.time.OffsetDateTime;

public record AvaliacaoCooperativa(
    Integer id,
    Integer visitaId,
    Integer agendamentoId,
    Integer condominioId,
    String nomeCondominio,
    int nota,
    String comentario,
    OffsetDateTime avaliadoEm
) {
}