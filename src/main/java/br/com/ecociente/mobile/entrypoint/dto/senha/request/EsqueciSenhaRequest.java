package br.com.ecociente.mobile.entrypoint.dto.senha.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para solicitar o envio do código de recuperação de senha")
public record EsqueciSenhaRequest(
    @io.swagger.v3.oas.annotations.media.Schema(description = "E-mail do usuário.", example = "pessoa@example.com", format = "email")
    @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email
) {
}