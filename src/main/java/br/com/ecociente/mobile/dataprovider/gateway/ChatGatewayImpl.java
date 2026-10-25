package br.com.ecociente.mobile.dataprovider.gateway;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.Conversa;
import br.com.ecociente.mobile.core.domain.MensagemConversa;
import br.com.ecociente.mobile.core.gateway.ChatGateway;
import br.com.ecociente.mobile.dataprovider.entity.ConversaEntity;
import br.com.ecociente.mobile.dataprovider.mapper.ChatPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.ConversaRepository;
import br.com.ecociente.mobile.dataprovider.repository.MensagemConversaRepository;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class ChatGatewayImpl implements ChatGateway {

  private final ConversaRepository conversaRepository;
  private final MensagemConversaRepository mensagemRepository;
  private final ChatPersistenceMapper mapper;

  public ChatGatewayImpl(
      ConversaRepository conversaRepository,
      MensagemConversaRepository mensagemRepository,
      ChatPersistenceMapper mapper) {
    this.conversaRepository = conversaRepository;
    this.mensagemRepository = mensagemRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<String> buscarLado(
      Integer usuarioId,
      Integer condominioId,
      Integer cooperativaId) {
    return conversaRepository.buscarLado(
        usuarioId,
        condominioId,
        cooperativaId);
  }

  @Override
  public Integer criarOuObter(
      Integer condominioId,
      Integer cooperativaId) {
    conversaRepository.inserirSeAusente(
        condominioId,
        cooperativaId);

    return conversaRepository.buscarIdPorPartes(
        condominioId,
        cooperativaId)
        .orElseThrow(() -> new IllegalStateException(
            "Não foi possível recuperar a conversa criada"));
  }

  @Override
  public Optional<Conversa> buscar(
      Integer conversaId,
      Integer usuarioId,
      boolean bloquear) {
    Optional<ConversaEntity> entity = bloquear
        ? conversaRepository.buscarComBloqueio(conversaId)
        : conversaRepository.findById(conversaId);

    return entity.map(conversa -> {
      var resumos = buscarResumos(
          usuarioId,
          List.of(conversa));

      return mapper.toDomain(
          conversa,
          obterResumo(resumos, conversa.getId()));
    });
  }

  @Override
  public long contarConversas(
      Integer usuarioId,
      boolean arquivadas) {
    return conversaRepository.contarDoParticipante(
        usuarioId,
        !arquivadas);
  }

  @Override
  public List<Conversa> listarConversas(
      Integer usuarioId,
      boolean arquivadas,
      long deslocamento,
      int limite) {
    var entities = conversaRepository.listarDoParticipante(
        usuarioId,
        !arquivadas,
        deslocamento,
        limite);

    if (entities.isEmpty()) {
      return List.of();
    }

    var resumos = buscarResumos(usuarioId, entities);

    return entities.stream()
        .map(entity -> mapper.toDomain(
            entity,
            obterResumo(resumos, entity.getId())))
        .toList();
  }

  @Override
  public long contarMensagens(Integer conversaId) {
    return mensagemRepository.countByConversaId(
        conversaId);
  }

  @Override
  public List<MensagemConversa> listarMensagens(
      Integer conversaId,
      long deslocamento,
      int limite) {
    return mensagemRepository.listarDaConversa(
        conversaId,
        deslocamento,
        limite)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public MensagemConversa enviar(
      Integer conversaId,
      Integer usuarioId,
      String lado,
      String conteudo) {
    // O service já adquiriu o bloqueio desta conversa.
    var conversa = exigirConversa(conversaId);

    var novaMensagem = mapper.toNovaMensagem(
        conversaId,
        usuarioId,
        lado,
        conteudo);

    var mensagemSalva = mensagemRepository.saveAndFlush(
        novaMensagem);

    conversa.setUltimaMensagemEm(
        mensagemSalva.getEnviadaEm());

    conversaRepository.save(conversa);

    return mapper.toDomain(mensagemSalva);
  }

  @Override
  public int marcarLeitura(
      Integer conversaId,
      String lado,
      Long ateMensagemId) {
    if (ateMensagemId == null) {
      return mensagemRepository.marcarTodasRecebidas(
          conversaId,
          lado);
    }

    return mensagemRepository.marcarRecebidasAte(
        conversaId,
        lado,
        ateMensagemId);
  }

  @Override
  public void definirAtiva(
      Integer conversaId,
      boolean ativa) {
    var conversa = exigirConversa(conversaId);

    conversa.setAtiva(ativa);

    conversaRepository.save(conversa);
  }

  private ConversaEntity exigirConversa(
      Integer conversaId) {
    return conversaRepository.findById(conversaId)
        .orElseThrow(() -> new IllegalStateException(
            "A conversa autorizada não foi encontrada"));
  }

  private Map<Integer, Object[]> buscarResumos(
      Integer usuarioId,
      List<ConversaEntity> conversas) {
    var ids = conversas.stream()
        .map(ConversaEntity::getId)
        .toList();

    return conversaRepository.buscarResumos(
        usuarioId,
        ids)
        .stream()
        .collect(Collectors.toMap(
            resultado -> ((Number) resultado[0]).intValue(),
            Function.identity()));
  }

  private Object[] obterResumo(
      Map<Integer, Object[]> resumos,
      Integer conversaId) {
    var resumo = resumos.get(conversaId);

    if (resumo == null) {
      throw new IllegalStateException(
          "Resumo da conversa não encontrado");
    }

    return resumo;
  }
}