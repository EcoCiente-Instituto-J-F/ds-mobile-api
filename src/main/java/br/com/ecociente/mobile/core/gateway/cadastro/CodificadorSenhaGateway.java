package br.com.ecociente.mobile.core.gateway.cadastro;

public interface CodificadorSenhaGateway {

    String gerarHash(String senha);
}