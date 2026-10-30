package br.com.ecociente.mobile.entrypoint.dto.senha.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
@Builder
@AllArgsConstructor
public class SenhaResponse {
    private Timestamp processadoEm;
}