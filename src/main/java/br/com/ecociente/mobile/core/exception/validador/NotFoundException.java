package br.com.ecociente.mobile.core.exception.validador;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String mensagem) {
        super(mensagem);
    }
}
