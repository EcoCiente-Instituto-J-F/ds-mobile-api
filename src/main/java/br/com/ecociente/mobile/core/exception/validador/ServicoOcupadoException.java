package br.com.ecociente.mobile.core.exception.validador;

public class ServicoOcupadoException extends RuntimeException {

    public ServicoOcupadoException() {
        super("Serviço ocupado; tente de novo.");
    }
}
