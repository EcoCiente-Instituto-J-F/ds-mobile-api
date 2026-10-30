package br.com.ecociente.mobile.core.gateway.senha;

public interface EmailGateway {

    void enviarRecuperacao(String destinatario, String token);
}