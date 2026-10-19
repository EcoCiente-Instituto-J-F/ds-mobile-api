package br.com.ecociente.mobile.core.domain;

import java.math.BigDecimal;

public record ResumoAvaliacoesCooperativa(
    long total,
    BigDecimal media
) {
}