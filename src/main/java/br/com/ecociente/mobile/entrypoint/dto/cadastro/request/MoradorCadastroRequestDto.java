package br.com.ecociente.mobile.entrypoint.dto.cadastro.request;

import org.hibernate.validator.constraints.br.CPF;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MoradorCadastroRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Nome completo do usuário.", example = "Pessoa Exemplo")
    @NotBlank(message = "Nome de usuário é obrigatorio")
  String nomeUsuario,

    @io.swagger.v3.oas.annotations.media.Schema(description = "E-mail do usuário.", example = "pessoa@example.com", format = "email")
    @Email(message = "Formato de email errado")
  @NotBlank(message = "Email é obrigatorio")
  String email,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Senha de acesso. No cadastro, deve atender às validações do campo.", example = "Exemplo@2026", format = "password", accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\sa-zA-Z\\d]).{8,}$", message = "A senha precisa conter no minímo uma letra maiuscula, uma minuscula, um caractere especial um número e 8 caracteres")
  @NotBlank(message = "senha é obrigatoria")
  @Size(min = 8)
  String senha,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Data de nascimento no formato ISO yyyy-MM-dd.", example = "2000-03-12", format = "date")
    @NotBlank(message = "Data de nascimento é obrigatoria")
  String dataNascimento,

    @io.swagger.v3.oas.annotations.media.Schema(description = "CPF do usuário, com ou sem pontuação.", example = "630.824.490-26")
    @CPF(message = "Formato do CPF errado")
  @NotBlank(message = "O CPF é obrigatorio")
  @Pattern(regexp = "^(\\d{3}\\.?){3}-?\\d{2}$", message = "O CPF pode ser preenchido com ou sem pontuação")
  String cpf,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Código de acesso fornecido pelo condomínio.", example = "CODIGO-EXEMPLO")
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