package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.ecociente.mobile.core.domain.pontuacao.AcaoReconciliacaoType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReconciliacaoPontuacaoResponse(
        Integer postagemId,
        AcaoReconciliacaoType acao,
        Integer pontos,
        Long movimentacaoId,
        RedisSincronizacaoResponse redis
) {
}