package br.com.ecociente.mobile.core.gateway.senha;

public interface FirebaseSenhaGateway {

  void atualizarSenha(String email, String novaSenha);
}