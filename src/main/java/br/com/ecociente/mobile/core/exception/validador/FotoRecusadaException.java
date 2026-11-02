package br.com.ecociente.mobile.core.exception.validador;

import br.com.ecociente.mobile.core.domain.validador.StatusTriagem;

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
