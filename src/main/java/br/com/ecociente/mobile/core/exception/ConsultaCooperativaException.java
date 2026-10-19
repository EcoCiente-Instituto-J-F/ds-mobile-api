package br.com.ecociente.mobile.core.exception;

public class ConsultaCooperativaException extends RuntimeException {

    public enum Tipo {
        DADOS_INVALIDOS,
        NAO_PERMITIDO,
        NAO_ENCONTRADO
    }

    private final Tipo tipo;
    private final String codigo;

    public ConsultaCooperativaException(
        Tipo tipo,
        String codigo,
        String mensagem
    ) {
        super(mensagem);
        this.tipo = tipo;
        this.codigo = codigo;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getCodigo() {
        return codigo;
    }
}