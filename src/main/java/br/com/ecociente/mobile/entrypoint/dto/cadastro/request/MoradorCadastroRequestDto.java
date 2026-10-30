package br.com.ecociente.mobile.entrypoint.dto.cadastro.request;

import org.hibernate.validator.constraints.br.CPF;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MoradorCadastroRequestDto(
  @Schema(example = "Emanuelly Mendes")
  @NotBlank(message = "Nome de usuário é obrigatorio")
  String nomeUsuario,

  @Schema(example = "emanuelly@gmail.com")
  @Email(message = "Formato de email errado")
  @NotBlank(message = "Email é obrigatorio")
  String email,

  @Schema(example = "Senha@T3ste")
  @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\sa-zA-Z\\d]).{8,}$", message = "A senha precisa conter no minímo uma letra maiuscula, uma minuscula, um caractere especial um número e 8 caracteres")
  @NotBlank(message = "senha é obrigatoria")
  @Size(min = 8)
  String senha,

  @Schema(example = "12/03/2024")
  @NotBlank(message = "Data de nascimento é obrigatoria")
  String dataNascimento,

  @Schema(example = "630.824.490-26")
  @CPF(message = "Formato do CPF errado")
  @NotBlank(message = "O CPF é obrigatorio")
  @Pattern(regexp = "^(\\d{3}\\.?){3}-?\\d{2}$", message = "O CPF pode ser preenchido com ou sem pontuação")
  String cpf,

  @NotBlank(message = "Código do condomínio é onrigatório")
  String codigoCondominio
) {
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejeitarCampoDesconhecido(
        String campo,
        Object valor
    ) {
        throw new IllegalArgumentException(
            "Campo não permitido: " + campo
        );
    }
}