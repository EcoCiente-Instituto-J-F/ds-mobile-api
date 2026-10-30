package br.com.ecociente.mobile.entrypoint.dto.pontuacao;

import java.util.List;

public record ErrorResponse(
    int status,
    String codigoError,
    List<ValidationError> details) {
}