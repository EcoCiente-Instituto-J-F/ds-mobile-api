package br.com.ecociente.mobile.core.exception.validador;

import br.com.ecociente.mobile.core.domain.validador.StatusTriagem;

/**
 * A triagem não produziu veredito por falha da foto ou do pedido: a resposta HTTP depende do status
 * (pedido_invalido = 422, foto_indisponivel = 502).
 */
public class FotoRecusadaException extends RuntimeException {

  private final StatusTriagem status;

  public FotoRecusadaException(StatusTriagem status, String mensagem) {
    super(mensagem);
    this.status = status;
  }

  public StatusTriagem getStatus() {
    return status;
  }
}
