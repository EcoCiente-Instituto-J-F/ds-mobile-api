package br.com.ecociente.mobile.entrypoint.dto.autenticacao;

import lombok.Builder;

@Builder
public record ValidationError(String field, String message) {
}
