package br.com.ecociente.mobile.core.exception.validador;

public class CategoriaInvalidaException extends RuntimeException {

    public CategoriaInvalidaException(String categoria) {
        super("Categoria fora do catálogo do validador: " + categoria);
    }
}
