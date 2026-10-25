package br.com.ecociente.mobile.dataprovider.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.Conversa;
import br.com.ecociente.mobile.core.domain.MensagemConversa;
import br.com.ecociente.mobile.dataprovider.entity.ConversaEntity;
import br.com.ecociente.mobile.dataprovider.entity.MensagemConversaEntity;

@Component
public class ChatPersistenceMapper {

  public Conversa toDomain(
      ConversaEntity entity,
      Object[] resumo) {
    return new Conversa(
        entity.getId(),
        entity.getCondominioId(),
        entity.getCooperativaId(),
        (String) resumo[1],
        (String) resumo[2],
        entity.getCriadaEm(),
        entity.getUltimaMensagemEm(),
        entity.isAtiva(),
        resumo[3] == null
            ? 0L
            : ((Number) resumo[3]).longValue());
  }

  public MensagemConversa toDomain(
      MensagemConversaEntity entity) {
    return new MensagemConversa(
        entity.getId(),
        entity.getConversaId(),
        entity.getRemetenteUsuarioId(),
        entity.getRemetenteTipo(),
        entity.getConteudo(),
        entity.getEnviadaEm(),
        entity.getLidaEm());
  }

  public MensagemConversaEntity toNovaMensagem(
      Integer conversaId,
      Integer usuarioId,
      String lado,
      String conteudo) {
    var entity = new MensagemConversaEntity();

    entity.setConversaId(conversaId);
    entity.setRemetenteUsuarioId(usuarioId);
    entity.setRemetenteTipo(lado);
    entity.setConteudo(conteudo);

    return entity;
  }
}