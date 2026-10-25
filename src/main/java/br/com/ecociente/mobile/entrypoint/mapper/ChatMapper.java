package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Conversa;
import br.com.ecociente.mobile.core.domain.MensagemConversa;
import br.com.ecociente.mobile.core.domain.PaginaChat;
import br.com.ecociente.mobile.entrypoint.dto.response.ChatResponses.ConversaDto;
import br.com.ecociente.mobile.entrypoint.dto.response.ChatResponses.MensagemDto;
import br.com.ecociente.mobile.entrypoint.dto.response.ChatResponses.PaginaDto;

@Component
public class ChatMapper {

  public ConversaDto conversa(Conversa conversa) {
    return new ConversaDto(
        conversa.id(),
        conversa.condominioId(),
        conversa.cooperativaId(),
        conversa.nomeCondominio(),
        conversa.nomeCooperativa(),
        conversa.criadaEm(),
        conversa.ultimaMensagemEm(),
        conversa.ativa(),
        conversa.naoLidas());
  }

  public MensagemDto mensagem(MensagemConversa mensagem) {
    return new MensagemDto(
        mensagem.id(),
        mensagem.conversaId(),
        mensagem.remetenteUsuarioId(),
        mensagem.remetenteTipo(),
        mensagem.conteudo(),
        mensagem.enviadaEm(),
        mensagem.lidaEm());
  }

  public PaginaDto<ConversaDto> conversas(
      PaginaChat<Conversa> pagina) {
    return new PaginaDto<>(
        pagina.total(),
        pagina.pagina(),
        pagina.tamanho(),
        pagina.totalPaginas(),
        pagina.itens().stream()
            .map(this::conversa)
            .toList());
  }

  public PaginaDto<MensagemDto> mensagens(
      PaginaChat<MensagemConversa> pagina) {
    return new PaginaDto<>(
        pagina.total(),
        pagina.pagina(),
        pagina.tamanho(),
        pagina.totalPaginas(),
        pagina.itens().stream()
            .map(this::mensagem)
            .toList());
  }
}