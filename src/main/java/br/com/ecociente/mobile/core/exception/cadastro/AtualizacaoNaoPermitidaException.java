package br.com.ecociente.mobile.core.exception.cadastro;

public class AtualizacaoNaoPermitidaException extends DomainException {

    public AtualizacaoNaoPermitidaException(String mensagem) {
        super("UPDATE_NOT_ALLOWED", mensagem);
    }
}