package br.com.ecociente.mobile.entrypoint.dto.autenticacao.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
  @Email @NotBlank String email,
  @NotBlank String senha
) {
}
