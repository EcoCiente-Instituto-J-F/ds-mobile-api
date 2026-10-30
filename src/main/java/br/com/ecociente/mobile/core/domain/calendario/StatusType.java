package br.com.ecociente.mobile.core.domain.calendario;

public enum StatusType {

  AGENDADO(1),
  CONFIRMADO(2),
  RECUSADO(3),
  CANCELADO(5),
  REALIZADO(4);

  private final int id;

  StatusType(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public static StatusType porId(int id) {
    return switch (id) {
      case 1, 14 -> AGENDADO;
      case 2, 12 -> CONFIRMADO;
      case 3, 15 -> RECUSADO;
      case 4, 13 -> REALIZADO;
      case 5, 11 -> CANCELADO;
      default -> throw new IllegalArgumentException(
          "Status de agendamento desconhecido: " + id);
    };
  }
}