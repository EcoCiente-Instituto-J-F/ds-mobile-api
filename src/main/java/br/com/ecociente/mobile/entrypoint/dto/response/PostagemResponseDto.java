package br.com.ecociente.mobile.entrypoint.dto.response;

import java.time.OffsetDateTime;

public record PostagemResponseDto(
    Integer id,
    Integer usuarioId,
    String autor,
    Integer condominioId,
    Integer torreId,
    Integer categoriaId,
    String categoria,
    String urlFoto,
    OffsetDateTime capturadaEm,
    OffsetDateTime dataPostagem,
    String statusValidacao,
    int saldoConfianca,
    OffsetDateTime dataLimiteAnalise,
    OffsetDateTime resolvidoEm) {
}
