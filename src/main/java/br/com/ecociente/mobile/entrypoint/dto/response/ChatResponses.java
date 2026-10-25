package br.com.ecociente.mobile.entrypoint.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public final class ChatResponses {
  private ChatResponses() {}
  public record ConversaDto(Integer id, Integer condominioId, Integer cooperativaId,
      String nomeCondominio, String nomeCooperativa, OffsetDateTime criadaEm,
      OffsetDateTime ultimaMensagemEm, boolean ativa, long naoLidas) {}
  public record MensagemDto(Long id, Integer conversaId, Integer remetenteUsuarioId,
      String remetenteTipo, String conteudo, OffsetDateTime enviadaEm, OffsetDateTime lidaEm) {}
  public record PaginaDto<T>(long total, int pagina, int tamanho, long totalPaginas, List<T> itens) {}
  public record LeituraDto(int mensagensMarcadas) {}
}
