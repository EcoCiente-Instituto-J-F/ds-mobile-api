package br.com.ecociente.mobile.core.domain.cadastro;

public record AtualizacaoCondominio(
    String nome,
    Integer tipoCondominioId,
    String urlFoto
) {
}