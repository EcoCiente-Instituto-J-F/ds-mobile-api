package br.com.ecociente.mobile.core.domain.validador;

import java.util.Locale;


/** ok = veredito válido; pedido_invalido = URL recusada; foto_indisponivel = o storage não entregou a foto. */
public enum StatusTriagem {

  OK,
  PEDIDO_INVALIDO,
  FOTO_INDISPONIVEL;

  public String getValor() {
    return name().toLowerCase(Locale.ROOT);
  }
}
