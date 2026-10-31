package br.com.ecociente.mobile.entrypoint.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class ChatRequests {

  private ChatRequests() {
  }

  public record Criar(
    @io.swagger.v3.oas.annotations.media.Schema(description = "ID numérico do condomínio existente no banco.", example = "1")
    @NotNull @Positive Integer condominioId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID numérico da cooperativa existente no banco.", example = "1")
    @NotNull @Positive Integer cooperativaId
) {
  }

  public record Enviar(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Texto da mensagem, entre 1 e 2000 caracteres após remover espaços nas extremidades.", example = "Podemos confirmar o horário da coleta?")
    @NotNull String conteudo
) {
  }

  public record Leitura(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Marca como lidas as mensagens recebidas até este ID, inclusive. Omissão marca todas as recebidas.", example = "10")
    @Positive Long ateMensagemId
) {
  }

  public record Arquivamento(
    @io.swagger.v3.oas.annotations.media.Schema(description = "true arquiva a conversa; false desarquiva. O arquivamento atual é compartilhado entre as partes.", example = "true")
    @NotNull Boolean arquivada
) {
  }
}