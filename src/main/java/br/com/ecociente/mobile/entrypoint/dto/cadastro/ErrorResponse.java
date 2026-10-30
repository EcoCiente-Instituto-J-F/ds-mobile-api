package br.com.ecociente.mobile.entrypoint.dto.cadastro;

import java.util.List;

import lombok.Builder;

@Builder 
public record ErrorResponse(
  int status, 
  String codigoError, 
  List<ValidationError> details) {
}
