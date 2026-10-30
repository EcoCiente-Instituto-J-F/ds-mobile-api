package br.com.ecociente.mobile.entrypoint.dto.autenticacao;

import java.util.List;

import lombok.Builder;

@Builder 
public record ErrorResponse(
  int status, 
  String codigoError, 
  List<ValidationError> details) {
}
