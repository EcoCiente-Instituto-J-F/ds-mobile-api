package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

import java.time.OffsetDateTime;

import br.com.ecociente.mobile.core.domain.pontuacao.OrigemPontuacaoType;
import br.com.ecociente.mobile.core.domain.pontuacao.TipoMovimentacaoType;

public record MovimentacaoPontosResponse(
    Long movimentacaoId,
    Integer usuarioId,
    Integer condominioId,
    Integer torreId,
    Integer categoriaId,
    OrigemPontuacaoType origemTipo,
    Integer postagemId,
    Integer tentativaQuizId,
    TipoMovimentacaoType tipoMovimentacao,
    Integer pontos,
    Long movimentacaoReferenciaId,
    OffsetDateTime ocorridoEm
) {
}