package br.com.ecociente.mobile.entrypoint.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class ChatRequests {
  private ChatRequests() {}
  public record Criar(@NotNull @Positive Integer condominioId, @NotNull @Positive Integer cooperativaId) {}
  public record Enviar(@NotNull String conteudo) {}
  public record Leitura(@Positive Long ateMensagemId) {}
  public record Arquivamento(@NotNull Boolean arquivada) {}
}
