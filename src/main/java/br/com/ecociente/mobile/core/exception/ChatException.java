package br.com.ecociente.mobile.core.exception;

public class ChatException extends RuntimeException {
  private final int status;
  private final String codigo;
  public ChatException(int status, String codigo, String mensagem) {
    super(mensagem);
    this.status = status;
    this.codigo = codigo;
  }
  public int getStatus() { return status; }
  public String getCodigo() { return codigo; }
}
