package br.com.ecociente.mobile.core.domain.calendario;

public record OrdenacaoCalendario(
        String propriedade,
        Direcao direcao,
        boolean ignorarMaiusculas,
        PosicaoNulos posicaoNulos) {

    public enum Direcao {
        ASC,
        DESC
    }

    public enum PosicaoNulos {
        NATIVE,
        NULLS_FIRST,
        NULLS_LAST
    }
}