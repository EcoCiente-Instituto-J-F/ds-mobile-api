package br.com.ecociente.mobile.core.gateway.pontuacao;

public interface VotoPostagemGateway {

    void registrar(
        Integer postagemId,
        Integer usuarioId,
        String tipoVoto,
        Integer motivoDenunciaId,
        String comentario
    );
}