package br.com.ecociente.mobile.core.domain.autenticacao;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Autenticacao {
  Integer id;
  Integer usuarioId;
  String token;
  String tipoToken;
  OffsetDateTime criadoEm;
  Integer expiradoEm;
  
}
