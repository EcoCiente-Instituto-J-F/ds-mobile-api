package br.com.ecociente.mobile.entrypoint.dto.senha.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Dados para alterar a senha do usuário autenticado")
public record AlterarSenhaRequest(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Senha atual do usuário autenticado.", example = "Atual@2026", format = "password", accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY)
    @NotBlank(message = "A senha atual é obrigatória")
        String senhaAtual,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Nova senha: 8 a 100 caracteres, maiúscula, minúscula, número e especial, sem espaços.", example = "NovaSenha@2026", format = "password", accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY)
    @NotBlank(message = "A nova senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{8,100}$",
                message = "A senha deve ter entre 8 e 100 caracteres, incluindo uma letra maiúscula, uma letra minúscula, um número e um caractere especial"
        )
        String novaSenha,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Deve ser igual à nova senha.", example = "NovaSenha@2026", format = "password", accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY)
    @NotBlank(message = "A confirmação da senha é obrigatória")
        String confirmacaoSenha
) {
}