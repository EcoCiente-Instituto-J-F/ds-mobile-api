package br.com.ecociente.mobile.entrypoint.dto.validador.response;

import br.com.ecociente.mobile.core.domain.validador.ResultadoVoto;

@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ResultadoVotoResponse(int saldoConfianca, boolean pontuacaoAtiva) {

  public static ResultadoVotoResponse de(ResultadoVoto resultado) {
    return new ResultadoVotoResponse(resultado.saldoConfianca(), resultado.pontuacaoAtiva());
  }
}
