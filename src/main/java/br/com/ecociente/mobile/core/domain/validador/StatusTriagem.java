package br.com.ecociente.mobile.core.domain.validador;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

/** ok = veredito válido; pedido_invalido = URL recusada; foto_indisponivel = o storage não entregou a foto. */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public enum StatusTriagem {

  OK,
  PEDIDO_INVALIDO,
  FOTO_INDISPONIVEL;

  @JsonValue
  public String getValor() {
    return name().toLowerCase(Locale.ROOT);
  }
}
