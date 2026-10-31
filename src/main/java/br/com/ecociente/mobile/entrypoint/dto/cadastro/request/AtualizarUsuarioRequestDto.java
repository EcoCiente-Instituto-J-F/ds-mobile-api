package br.com.ecociente.mobile.entrypoint.dto.cadastro.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Nome a atualizar. Quando omitido, mantém o valor atual.", example = "Nome atualizado")
    @Size(min = 1, max = 100)
    @Pattern(
        regexp = "(?s).*\\S.*",
        message = "Nome não pode estar em branco"
    )
    String nome,

    @io.swagger.v3.oas.annotations.media.Schema(description = "E-mail do usuário.", example = "pessoa@example.com", format = "email")
    @Email(message = "E-mail inválido")
    @Size(min = 1, max = 255)
    @Pattern(
        regexp = "\\S+",
        message = "E-mail não pode estar em branco ou conter espaços"
    )
    String email,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Data de nascimento no formato ISO yyyy-MM-dd.", example = "2000-03-12", format = "date")
    @Past(message = "Data de nascimento deve ser anterior a hoje")
    LocalDate dataNascimento,

    @io.swagger.v3.oas.annotations.media.Schema(description = "URL HTTP ou HTTPS do avatar. Na atualização, string vazia remove o avatar; omissão mantém o atual.", example = "https://storage.example.com/avatar.jpg")
    @Size(max = 500)
    @Pattern(
        regexp = "^$|(?i:https?)://\\S+",
        message = "Informe uma URL HTTP ou HTTPS"
    )
    String urlAvatar,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Telefone com DDD, somente 10 ou 11 dígitos.", example = "11999999999")
    @Pattern(
        regexp = "^\\d{10,11}$",
        message = "Telefone deve conter 10 ou 11 dígitos"
    )
    String telefone,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID do telefone pertencente ao usuário. Necessário para selecionar um telefone quando houver vários ativos.", example = "1")
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