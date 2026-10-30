package br.com.ecociente.mobile.entrypoint.dto.senha.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Dados para alterar a senha do usuário autenticado")
public record AlterarSenhaRequest(

        @Schema(example = "Atual@12345")
        @NotBlank(message = "A senha atual é obrigatória")
        String senhaAtual,

        @Schema(example = "Nova@12345")
        @NotBlank(message = "A nova senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{8,100}$",
                message = "A senha deve ter entre 8 e 100 caracteres, incluindo uma letra maiúscula, uma letra minúscula, um número e um caractere especial"
        )
        String novaSenha,

        @Schema(example = "Nova@12345")
        @NotBlank(message = "A confirmação da senha é obrigatória")
        String confirmacaoSenha

) {
}