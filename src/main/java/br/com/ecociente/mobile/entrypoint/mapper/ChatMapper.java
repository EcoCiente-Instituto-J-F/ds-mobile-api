package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;
import br.com.ecociente.mobile.core.domain.*;
import br.com.ecociente.mobile.entrypoint.dto.response.ChatResponses.*;

@Component
public class ChatMapper {
  public ConversaDto conversa(Conversa c) {
    return new ConversaDto(c.id(), c.condominioId(), c.cooperativaId(), c.nomeCondominio(),
        c.nomeCooperativa(), c.criadaEm(), c.ultimaMensagemEm(), c.ativa(), c.naoLidas());
  }
  public MensagemDto mensagem(MensagemConversa m) {
    return new MensagemDto(m.id(), m.conversaId(), m.remetenteUsuarioId(), m.remetenteTipo(),
        m.conteudo(), m.enviadaEm(), m.lidaEm());
  }
  public PaginaDto<ConversaDto> conversas(PaginaChat<Conversa> p) {
    return new PaginaDto<>(p.total(), p.pagina(), p.tamanho(), p.totalPaginas(), p.itens().stream().map(this::conversa).toList());
  }
  public PaginaDto<MensagemDto> mensagens(PaginaChat<MensagemConversa> p) {
    return new PaginaDto<>(p.total(), p.pagina(), p.tamanho(), p.totalPaginas(), p.itens().stream().map(this::mensagem).toList());
  }
}
