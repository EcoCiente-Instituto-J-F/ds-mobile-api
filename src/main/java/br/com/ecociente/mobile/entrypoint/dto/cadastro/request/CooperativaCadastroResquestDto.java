package br.com.ecociente.mobile.entrypoint.dto.cadastro.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CooperativaCadastroResquestDto(

  @Schema(example = "Emanuelly Mendes")
  @NotBlank(message ="Nome do responsável é obrigatório")
  String nomeResponsavel,

  @Schema(example = "emanuellySantos@gmail.com")
  @NotBlank(message = "Email é obrigatório")
  @Email(message = "Formato de email inválido")
  String email,

  @Schema(example = "Senha@T3ste")
  @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\sa-zA-Z\\d]).{8,}$", message = "A senha precisa conter no minímo uma letra maiuscula, uma minuscula, um caractere especial um número e 8 caracteres")
  @NotBlank(message = "senha é obrigatoria")
  @Size(min = 8)
  String senha,

  @Schema(example = "EcoCoop")
  @NotBlank(message ="Nome do responsável é obrigatório")
  String nomeCooperativa,

  @Schema(example = "54934758000126")
  @NotBlank(message = "CNPJ é obrigatório")
  @Pattern(
    regexp = "(?i)^(?:[a-z0-9]{12}[0-9]{2}|[a-z0-9]{2}\\.[a-z0-9]{3}\\.[a-z0-9]{3}/[a-z0-9]{4}-[0-9]{2})$",
    message = "CNPJ deve conter 14 caracteres, com ou sem pontuação"
  )
  String cnpj,

  @Schema(example = "ecoCoop@gmail.com")
  @NotBlank(message = "Email é obrigatório")
  @Email(message = "Formato de email inválido")
  String emailCooperativa,

  @Schema(example = "11953762717")
  @NotBlank(message = "Telefone é obrigatório")
  @Pattern(regexp = "^\\d{10,11}$",message = "Telefone deve conter 10 ou 11 dígitos")
  String telefone,

  @Schema(example = "05280130")
  @Pattern(regexp = "^\\d{5}-?\\d{3}$",message = "O CEP pode ser preenchido com ou sem pontuação")
  @NotBlank
  String cep,

  @Schema (example = "41")
  @NotBlank(message = "o número é obrigatorio")
  String numero,

  @Schema(example = "Apartamento 01")
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