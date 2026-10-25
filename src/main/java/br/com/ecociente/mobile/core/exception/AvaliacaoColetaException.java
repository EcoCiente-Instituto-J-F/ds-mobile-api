package br.com.ecociente.mobile.core.exception;

public class AvaliacaoColetaException extends RuntimeException {

  public enum Tipo {
    DADOS_INVALIDOS,
    NAO_PERMITIDO,
    NAO_ENCONTRADO,
    CONFLITO
  }

  private final Tipo tipo;
  private final String codigo;

  public AvaliacaoColetaException(
      Tipo tipo,
      String codigo,
      String mensagem) {
    super(mensagem);
    this.tipo = tipo;
    this.codigo = codigo;
  }

  /** Usada pelo serviço e pelo gateway, que traduz a violação da unicidade do banco. */
  public static AvaliacaoColetaException jaAvaliada() {
    return new AvaliacaoColetaException(
        Tipo.CONFLITO,
        "AVALIACAO_JA_REGISTRADA",
        "Esta coleta já foi avaliada");
  }

  public Tipo getTipo() {
    return tipo;
  }

  public String getCodigo() {
    return codigo;
  }
}
