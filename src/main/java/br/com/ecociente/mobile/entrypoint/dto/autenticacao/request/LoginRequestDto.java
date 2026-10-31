package br.com.ecociente.mobile.entrypoint.dto.autenticacao.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "E-mail do usuário.", example = "pessoa@example.com", format = "email")
    @Email @NotBlank String email,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Senha de acesso. No cadastro, deve atender às validações do campo.", example = "Exemplo@2026", format = "password", accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY)
    @NotBlank String senha
) {
}
