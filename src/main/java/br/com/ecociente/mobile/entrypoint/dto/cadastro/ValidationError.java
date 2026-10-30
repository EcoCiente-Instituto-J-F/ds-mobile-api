package br.com.ecociente.mobile.entrypoint.dto.cadastro;

import lombok.Builder;

/**
 * ValidationError
 */
@Builder
public record ValidationError(
  String field, 
  String message
) {

}
