package br.com.ecociente.mobile.entrypoint.dto.cadastro.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequestDto(

    @Size(min = 1, max = 100)
    @Pattern(
        regexp = "(?s).*\\S.*",
        message = "Nome não pode estar em branco"
    )
    String nome,

    @Email(message = "E-mail inválido")
    @Size(min = 1, max = 255)
    @Pattern(
        regexp = "\\S+",
        message = "E-mail não pode estar em branco ou conter espaços"
    )
    String email,

    @Past(message = "Data de nascimento deve ser anterior a hoje")
    LocalDate dataNascimento,

    @Size(max = 500)
    @Pattern(
        regexp = "^$|(?i:https?)://\\S+",
        message = "Informe uma URL HTTP ou HTTPS"
    )
    String urlAvatar,

    @Pattern(
        regexp = "^\\d{10,11}$",
        message = "Telefone deve conter 10 ou 11 dígitos"
    )
    String telefone,

    @Positive
    Integer telefoneId

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