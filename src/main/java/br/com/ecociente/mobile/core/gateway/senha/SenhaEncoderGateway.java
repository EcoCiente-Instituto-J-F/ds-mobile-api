package br.com.ecociente.mobile.core.gateway.senha;

public interface SenhaEncoderGateway {

    String gerarHash(String senha);

    boolean corresponde(String senha, String senhaHash);
}