package br.com.ecociente.mobile.core.domain.autenticacao;

import java.util.List;

public enum PerfilUsuarioType {

  USUARIO_COMUM(
      List.of("comum", "usuario comum", "usuário comum")),

  MORADOR(
      List.of("morador", "moradora")),

  SINDICO(
      List.of("sindico", "síndico")),

  COOPERATIVA(
      List.of("cooperativa")),

  ADMIN(
      List.of("admin", "administrador", "administradora"));

  private final List<String> nomesTipo;

  PerfilUsuarioType(List<String> nomesTipo) {
    this.nomesTipo = nomesTipo;
  }

  public List<String> getNomesTipo() {
    return nomesTipo;
  }
}