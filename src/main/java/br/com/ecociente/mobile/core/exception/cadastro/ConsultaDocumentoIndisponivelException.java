package br.com.ecociente.mobile.core.exception.cadastro;

public class ConsultaDocumentoIndisponivelException extends DomainException {

    public ConsultaDocumentoIndisponivelException() {
        super(
            "DOCUMENT_QUERY_UNAVAILABLE",
            "Não foi possível verificar o documento. Tente novamente mais tarde."
        );
    }
}