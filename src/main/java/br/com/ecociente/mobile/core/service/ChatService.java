package br.com.ecociente.mobile.core.service;

import java.util.List;

import org.springframework.stereotype.Service;
import br.com.ecociente.mobile.core.service.notificacoes.PushService;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.Conversa;
import br.com.ecociente.mobile.core.domain.MensagemConversa;
import br.com.ecociente.mobile.core.domain.PaginaChat;
import br.com.ecociente.mobile.core.exception.ChatException;
import br.com.ecociente.mobile.core.gateway.ChatGateway;

@Service
public class ChatService {

  private final ChatGateway gateway;
  private final PushService push;

  public ChatService(ChatGateway gateway, PushService push) {
    this.gateway = gateway;
    this.push = push;
  }

  @Transactional
  public Conversa criar(
      Integer usuarioId,
      Integer condominioId,
      Integer cooperativaId) {
    validarUsuario(usuarioId);
    validarId(condominioId);
    validarId(cooperativaId);

    buscarLado(usuarioId, condominioId, cooperativaId);

    Integer conversaId = gateway.criarOuObter(
        condominioId,
        cooperativaId);

    // Repetir a criação não desarquiva a conversa.
    return gateway.buscar(conversaId, usuarioId, false)
        .orElseThrow(() -> erro(
            404,
            "CONVERSA_NAO_ENCONTRADA",
            "Conversa não encontrada"));
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaChat<Conversa> listar(
      Integer usuarioId,
      boolean arquivadas,
      int pagina,
      int tamanho) {
    validarUsuario(usuarioId);
    validarPaginacao(pagina, tamanho);

    long total = gateway.contarConversas(
        usuarioId,
        arquivadas);

    var itens = gateway.listarConversas(
        usuarioId,
        arquivadas,
        (long) pagina * tamanho,
        tamanho);

    return montarPagina(total, pagina, tamanho, itens);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaChat<MensagemConversa> mensagens(
      Integer usuarioId,
      Integer conversaId,
      int pagina,
      int tamanho) {
    validarPaginacao(pagina, tamanho);
    autorizar(usuarioId, conversaId, false);

    long total = gateway.contarMensagens(conversaId);

    var itens = gateway.listarMensagens(
        conversaId,
        (long) pagina * tamanho,
        tamanho);

    return montarPagina(total, pagina, tamanho, itens);
  }

  @Transactional
  public MensagemConversa enviar(
      Integer usuarioId,
      Integer conversaId,
      String conteudo) {
    String texto = conteudo == null
        ? ""
        : conteudo.strip();

    int caracteres = texto.codePointCount(
        0,
        texto.length());

    if (texto.isEmpty() || caracteres > 2000) {
      throw erro(
          400,
          "MENSAGEM_INVALIDA",
          "A mensagem deve conter entre 1 e 2000 caracteres");
    }

    var conversa = autorizar(
        usuarioId,
        conversaId,
        true);

    if (!conversa.ativa()) {
      throw erro(
          409,
          "CONVERSA_ARQUIVADA",
          "Desarquive a conversa para enviar mensagens");
    }

    String lado = buscarLado(
        usuarioId,
        conversa.condominioId(),
        conversa.cooperativaId());

    return gateway.enviar(
        conversaId,
        usuarioId,
        lado,
        texto);
  }

  @Transactional
  public int ler(
      Integer usuarioId,
      Integer conversaId,
      Long ateMensagemId) {
    if (ateMensagemId != null && ateMensagemId <= 0) {
      throw erro(
          400,
          "MENSAGEM_ID_INVALIDO",
          "Informe um ID de mensagem positivo");
    }

    var conversa = autorizar(
        usuarioId,
        conversaId,
        true);

    String lado = buscarLado(
        usuarioId,
        conversa.condominioId(),
        conversa.cooperativaId());

    return gateway.marcarLeitura(
        conversaId,
        lado,
        ateMensagemId);
  }

  @Transactional
  public void arquivar(
      Integer usuarioId,
      Integer conversaId,
      boolean arquivada) {
    autorizar(usuarioId, conversaId, true);
    gateway.definirAtiva(conversaId, !arquivada);
  }

  private Conversa autorizar(
      Integer usuarioId,
      Integer conversaId,
      boolean bloquear) {
    validarUsuario(usuarioId);
    validarId(conversaId);

    var conversa = gateway.buscar(
        conversaId,
        usuarioId,
        bloquear).orElseThrow(
            () -> erro(
                404,
                "CONVERSA_NAO_ENCONTRADA",
                "Conversa não encontrada"));

    buscarLado(
        usuarioId,
        conversa.condominioId(),
        conversa.cooperativaId());

    return conversa;
  }

  private String buscarLado(
      Integer usuarioId,
      Integer condominioId,
      Integer cooperativaId) {
    return gateway.buscarLado(
        usuarioId,
        condominioId,
        cooperativaId)
        .filter(lado -> "CONDOMINIO".equals(lado)
            || "COOPERATIVA".equals(lado))
        .orElseThrow(() -> erro(
            403,
            "CHAT_NAO_PERMITIDO",
            "Você não representa uma das partes ativas desta conversa"));
  }

  private void validarUsuario(Integer usuarioId) {
    if (usuarioId == null || usuarioId <= 0) {
      throw erro(
          403,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }
  }

  private void validarId(Integer id) {
    if (id == null || id <= 0) {
      throw erro(
          400,
          "ID_INVALIDO",
          "Informe um ID positivo");
    }
  }

  private void validarPaginacao(
      int pagina,
      int tamanho) {
    if (pagina < 0 || tamanho < 1 || tamanho > 100) {
      throw erro(
          400,
          "PAGINACAO_INVALIDA",
          "Página a partir de zero e tamanho entre 1 e 100");
    }
  }

  private <T> PaginaChat<T> montarPagina(
      long total,
      int pagina,
      int tamanho,
      List<T> itens) {
    long totalPaginas = total / tamanho
        + (total % tamanho == 0 ? 0 : 1);

    return new PaginaChat<>(
        total,
        pagina,
        tamanho,
        totalPaginas,
        itens);
  }

  private ChatException erro(
      int status,
      String codigo,
      String mensagem) {
    return new ChatException(status, codigo, mensagem);
  }
}