package br.com.ecociente.mobile.core.domain.cadastro;

import java.util.Locale;

import br.com.ecociente.mobile.core.exception.cadastro.BusinessException;

public final class Cnpj {

    private Cnpj() {
    }

    public static String normalizarEValidar(String valor) {
        if (valor == null) {
            throw invalido();
        }

        String cnpj = valor.trim().toUpperCase(Locale.ROOT);

        String formato =
            "(?:[A-Z0-9]{12}[0-9]{2}"
            + "|[A-Z0-9]{2}\\.[A-Z0-9]{3}\\.[A-Z0-9]{3}/[A-Z0-9]{4}-[0-9]{2})";

        if (!cnpj.matches(formato)) {
            throw invalido();
        }

        cnpj = cnpj
            .replace(".", "")
            .replace("/", "")
            .replace("-", "");

        if (cnpj.matches("([0-9])\\1{13}")) {
            throw invalido();
        }

        int primeiroDigito = calcularDigito(cnpj.substring(0, 12));

        int segundoDigito = calcularDigito(
            cnpj.substring(0, 12) + primeiroDigito
        );

        if (
            cnpj.charAt(12) - '0' != primeiroDigito
            || cnpj.charAt(13) - '0' != segundoDigito
        ) {
            throw invalido();
        }

        return cnpj;
    }

    private static int calcularDigito(String base) {
        int soma = 0;
        int peso = 2;

        for (int i = base.length() - 1; i >= 0; i--) {
            soma += (base.charAt(i) - 48) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }

        int resto = soma % 11;

        return resto < 2 ? 0 : 11 - resto;
    }

    private static BusinessException invalido() {
        return new BusinessException(
            "CNPJ_INVALID",
            "CNPJ inválido"
        );
    }
}