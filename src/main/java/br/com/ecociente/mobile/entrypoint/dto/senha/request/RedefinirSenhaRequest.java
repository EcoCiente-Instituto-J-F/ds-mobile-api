package br.com.ecociente.mobile.entrypoint.dto.senha.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Dados para redefinir a senha usando o código recebido por e-mail")
public record RedefinirSenhaRequest(
    @io.swagger.v3.oas.annotations.media.Schema(description = "E-mail do usuário.", example = "pessoa@example.com", format = "email")
    @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Código de quatro dígitos recebido por e-mail, válido por 15 minutos e de uso único. Preserve zeros iniciais.", example = "0123")
    @NotBlank(message = "O código é obrigatório")
        @Pattern(
                regexp = "^[0-9]{4}$",
                message = "O código deve conter exatamente 4 dígitos"
        )
        String token,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Nova senha: 8 a 100 caracteres, maiúscula, minúscula, número e especial, sem espaços.", example = "NovaSenha@2026", format = "password", accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY)
    @NotBlank(message = "A nova senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{8,100}$",
                message = "A senha deve ter entre 8 e 100 caracteres, incluindo maiúscula, minúscula, número e caractere especial"
        )
        String novaSenha,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Deve ser igual à nova senha.", example = "NovaSenha@2026", format = "password", accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY)
    @NotBlank(message = "A confirmação da senha é obrigatória")
        String confirmacaoSenha
) {
} 