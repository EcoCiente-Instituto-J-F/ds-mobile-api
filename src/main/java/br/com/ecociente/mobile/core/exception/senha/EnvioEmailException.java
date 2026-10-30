package br.com.ecociente.mobile.core.exception.senha;

public class EnvioEmailException extends RuntimeException {

    public EnvioEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}