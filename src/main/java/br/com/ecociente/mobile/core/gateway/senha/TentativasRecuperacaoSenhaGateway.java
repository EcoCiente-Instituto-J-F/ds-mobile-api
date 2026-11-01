package br.com.ecociente.mobile.core.gateway.senha;

import java.time.Duration;

/**
 * Conta os códigos de recuperação errados por e-mail, para barrar força bruta no código de 4 dígitos.
 * A chave é o e-mail (normalizado), não o usuário: assim e-mail inexistente também é contado
 * e a resposta não revela se a conta existe.
 */
public interface TentativasRecuperacaoSenhaGateway {

  /** Falhas registradas na janela atual (0 quando não há nenhuma). */
  int falhas(String email);

  /** Registra uma falha e devolve o total; a janela começa na primeira falha e dura {@code janela}. */
  int registrarFalha(String email, Duration janela);

  void limpar(String email);
}
