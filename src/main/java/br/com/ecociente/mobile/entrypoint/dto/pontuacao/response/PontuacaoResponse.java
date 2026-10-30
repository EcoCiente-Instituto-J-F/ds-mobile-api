package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.ecociente.mobile.core.domain.pontuacao.OrigemPontuacaoType;
import br.com.ecociente.mobile.core.domain.pontuacao.TipoMovimentacaoType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PontuacaoResponse(
        OrigemPontuacaoType origemTipo,
        Integer postagemId,
        Integer tentativaId,
        Integer usuarioId,
        Integer categoriaId,
        Integer pontosConcedidos,
        TipoMovimentacaoType tipoMovimentacao,
        Long movimentacaoId,
        String motivo,
        RedisSincronizacaoResponse redis
) {
}