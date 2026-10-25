package br.com.ecociente.mobile.core.gateway;

import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.Conversa;
import br.com.ecociente.mobile.core.domain.MensagemConversa;

public interface ChatGateway {

  Optional<String> buscarLado(
      Integer usuarioId,
      Integer condominioId,
      Integer cooperativaId);

  Integer criarOuObter(
      Integer condominioId,
      Integer cooperativaId);

  Optional<Conversa> buscar(
      Integer conversaId,
      Integer usuarioId,
      boolean bloquear);

  long contarConversas(
      Integer usuarioId,
      boolean arquivadas);

  List<Conversa> listarConversas(
      Integer usuarioId,
      boolean arquivadas,
      long deslocamento,
      int limite);

  long contarMensagens(Integer conversaId);

  List<MensagemConversa> listarMensagens(
      Integer conversaId,
      long deslocamento,
      int limite);

  MensagemConversa enviar(
      Integer conversaId,
      Integer usuarioId,
      String lado,
      String conteudo);

  int marcarLeitura(
      Integer conversaId,
      String lado,
      Long ateMensagemId);

  void definirAtiva(
      Integer conversaId,
      boolean ativa);
}