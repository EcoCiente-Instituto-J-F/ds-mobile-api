package br.com.ecociente.mobile.entrypoint.dto.cadastro.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CooperativaCadastroResquestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Nome completo do responsável pela cooperativa.", example = "Pessoa Exemplo")
    @NotBlank(message ="Nome do responsável é obrigatório")
  String nomeResponsavel,

    @io.swagger.v3.oas.annotations.media.Schema(description = "E-mail do usuário.", example = "pessoa@example.com", format = "email")
    @NotBlank(message = "Email é obrigatório")
  @Email(message = "Formato de email inválido")
  String email,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Senha de acesso. No cadastro, deve atender às validações do campo.", example = "Exemplo@2026", format = "password", accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\sa-zA-Z\\d]).{8,}$", message = "A senha precisa conter no minímo uma letra maiuscula, uma minuscula, um caractere especial um número e 8 caracteres")
  @NotBlank(message = "senha é obrigatoria")
  @Size(min = 8)
  String senha,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Nome da cooperativa.", example = "Cooperativa Exemplo")
    @NotBlank(message ="Nome do responsável é obrigatório")
  String nomeCooperativa,

    @io.swagger.v3.oas.annotations.media.Schema(description = "CNPJ da cooperativa, com ou sem pontuação. Consultado e validado durante o cadastro.", example = "54934758000126")
    @NotBlank(message = "CNPJ é obrigatório")
  @Pattern(
    regexp = "(?i)^(?:[a-z0-9]{12}[0-9]{2}|[a-z0-9]{2}\\.[a-z0-9]{3}\\.[a-z0-9]{3}/[a-z0-9]{4}-[0-9]{2})$",
    message = "CNPJ deve conter 14 caracteres, com ou sem pontuação"
  )
  String cnpj,

    @io.swagger.v3.oas.annotations.media.Schema(description = "E-mail de contato da cooperativa.", example = "contato@example.com", format = "email")
    @NotBlank(message = "Email é obrigatório")
  @Email(message = "Formato de email inválido")
  String emailCooperativa,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Telefone com DDD, somente 10 ou 11 dígitos.", example = "11999999999")
    @NotBlank(message = "Telefone é obrigatório")
  @Pattern(regexp = "^\\d{10,11}$",message = "Telefone deve conter 10 ou 11 dígitos")
  String telefone,

    @io.swagger.v3.oas.annotations.media.Schema(description = "CEP com oito dígitos, com ou sem hífen.", example = "01001000")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$",message = "O CEP pode ser preenchido com ou sem pontuação")
  @NotBlank
  String cep,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Número do endereço.", example = "10")
    @NotBlank(message = "o número é obrigatorio")
  String numero,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Complemento do endereço.", example = "Apartamento 12")
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