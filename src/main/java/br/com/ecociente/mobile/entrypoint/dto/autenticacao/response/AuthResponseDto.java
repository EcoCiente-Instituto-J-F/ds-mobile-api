package br.com.ecociente.mobile.entrypoint.dto.autenticacao.response;

import br.com.ecociente.mobile.core.domain.autenticacao.PerfilUsuarioType;

public record AuthResponseDto(
  String token,
  String tipoToken,
  Long expiraEmSegundos,
  Integer usuarioId,
  String nome,
  String email,
  PerfilUsuarioType perfil
) {
}
