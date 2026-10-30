package br.com.ecociente.mobile.entrypoint.dto.senha.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Dados para redefinir a senha usando o código recebido por e-mail")
public record RedefinirSenhaRequest(

        @Schema(example = "usuario@example.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email,

        @Schema(example = "0427")
        @NotBlank(message = "O código é obrigatório")
        @Pattern(
                regexp = "^[0-9]{4}$",
                message = "O código deve conter exatamente 4 dígitos"
        )
        String token,

        @Schema(example = "Nova@12345")
        @NotBlank(message = "A nova senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{8,100}$",
                message = "A senha deve ter entre 8 e 100 caracteres, incluindo maiúscula, minúscula, número e caractere especial"
        )
        String novaSenha,

        @Schema(example = "Nova@12345")
        @NotBlank(message = "A confirmação da senha é obrigatória")
        String confirmacaoSenha

) {
} 