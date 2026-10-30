package br.com.ecociente.mobile.entrypoint.dto.cadastro.request;

import org.hibernate.validator.constraints.br.CPF;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ComumCadastroRequestDto(

  /**record de request para usuário comum
   * Diferentes interfaces fazem diferentes tipos de cadastro
   */

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

  @Schema(example = "05280130")
  @Pattern(regexp = "^\\d{5}-?\\d{3}$",message = "O CEP pode ser preenchido com ou sem pontuação")
  @NotBlank
  String cep,

  @Schema(example = "41")
  @NotBlank(message = "o número é obrigatorio")
  String numero,

  @Schema(example = "Apartamento 02")
  String complemento
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