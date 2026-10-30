package br.com.ecociente.mobile.core.domain.autenticacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Usuario {
  Integer id;
  String nome;
  String email;
  String senhaHash;
  Boolean ativo;
  String tipoUsuario;
}
