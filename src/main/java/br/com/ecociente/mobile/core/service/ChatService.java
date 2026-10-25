package br.com.ecociente.mobile.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import br.com.ecociente.mobile.core.domain.*;
import br.com.ecociente.mobile.core.exception.ChatException;
import br.com.ecociente.mobile.core.gateway.ChatGateway;

@Service
public class ChatService {
  private final ChatGateway gateway;
  public ChatService(ChatGateway gateway) { this.gateway = gateway; }

  @Transactional
  public Conversa criar(Integer usuarioId, Integer condominioId, Integer cooperativaId) {
    usuario(usuarioId); positivo(condominioId); positivo(cooperativaId);
    lado(usuarioId, condominioId, cooperativaId);
    var id = gateway.criarOuObter(condominioId, cooperativaId);
    // Não reativa uma conversa arquivada em um POST repetido.
    return gateway.buscar(id, usuarioId, false).orElseThrow(() -> erro(404, "CONVERSA_NAO_ENCONTRADA", "Conversa não encontrada"));
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaChat<Conversa> listar(Integer usuarioId, boolean arquivadas, int pagina, int tamanho) {
    usuario(usuarioId); paginacao(pagina, tamanho);
    long total = gateway.contarConversas(usuarioId, arquivadas);
    return pagina(total, pagina, tamanho, gateway.listarConversas(usuarioId, arquivadas, (long) pagina * tamanho, tamanho));
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaChat<MensagemConversa> mensagens(Integer usuarioId, Integer conversaId, int pagina, int tamanho) {
    paginacao(pagina, tamanho);
    autorizar(usuarioId, conversaId, false);
    long total = gateway.contarMensagens(conversaId);
    return pagina(total, pagina, tamanho, gateway.listarMensagens(conversaId, (long) pagina * tamanho, tamanho));
  }

  @Transactional
  public MensagemConversa enviar(Integer usuarioId, Integer conversaId, String conteudo) {
    if (conteudo == null || conteudo.strip().isEmpty() || conteudo.strip().codePointCount(0, conteudo.strip().length()) > 2000) {
      throw erro(400, "MENSAGEM_INVALIDA", "A mensagem deve conter entre 1 e 2000 caracteres");
    }
    var conversa = autorizar(usuarioId, conversaId, true);
    if (!conversa.ativa()) throw erro(409, "CONVERSA_ARQUIVADA", "Desarquive a conversa para enviar mensagens");
    return gateway.enviar(conversaId, usuarioId,
        lado(usuarioId, conversa.condominioId(), conversa.cooperativaId()), conteudo.strip());
  }

  @Transactional
  public int ler(Integer usuarioId, Integer conversaId, Long ateMensagemId) {
    if (ateMensagemId != null && ateMensagemId <= 0) throw erro(400, "MENSAGEM_ID_INVALIDO", "Informe um ID de mensagem positivo");
    var conversa = autorizar(usuarioId, conversaId, true);
    return gateway.marcarLeitura(conversaId,
        lado(usuarioId, conversa.condominioId(), conversa.cooperativaId()), ateMensagemId);
  }

  @Transactional
  public void arquivar(Integer usuarioId, Integer conversaId, boolean arquivada) {
    autorizar(usuarioId, conversaId, true);
    gateway.definirAtiva(conversaId, !arquivada);
  }

  private Conversa autorizar(Integer usuarioId, Integer conversaId, boolean bloquear) {
    usuario(usuarioId); positivo(conversaId);
    var c = gateway.buscar(conversaId, usuarioId, bloquear)
        .orElseThrow(() -> erro(404, "CONVERSA_NAO_ENCONTRADA", "Conversa não encontrada"));
    lado(usuarioId, c.condominioId(), c.cooperativaId());
    return c;
  }
  private String lado(Integer usuarioId, Integer condominioId, Integer cooperativaId) {
    return gateway.buscarLado(usuarioId, condominioId, cooperativaId)
        .filter(l -> l.equals("CONDOMINIO") || l.equals("COOPERATIVA"))
        .orElseThrow(() -> erro(403, "CHAT_NAO_PERMITIDO", "Você não representa uma das partes ativas desta conversa"));
  }
  private void usuario(Integer id) {
    if (id == null || id <= 0) throw erro(403, "USUARIO_INVALIDO", "Usuário inválido");
  }
  private void positivo(Integer id) {
    if (id == null || id <= 0) throw erro(400, "ID_INVALIDO", "Informe um ID positivo");
  }
  private void paginacao(int pagina, int tamanho) {
    if (pagina < 0 || tamanho < 1 || tamanho > 100) throw erro(400, "PAGINACAO_INVALIDA", "Página a partir de zero; tamanho entre 1 e 100");
  }
  private <T> PaginaChat<T> pagina(long total, int pagina, int tamanho, java.util.List<T> itens) {
    return new PaginaChat<>(total, pagina, tamanho, total / tamanho + (total % tamanho == 0 ? 0 : 1), itens);
  }
  private ChatException erro(int status, String codigo, String mensagem) { return new ChatException(status, codigo, mensagem); }
}
