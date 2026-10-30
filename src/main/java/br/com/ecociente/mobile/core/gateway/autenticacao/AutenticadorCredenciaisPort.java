package br.com.ecociente.mobile.core.gateway.autenticacao;

public interface AutenticadorCredenciaisPort {
  void autenticar(String email, String senha);
}
