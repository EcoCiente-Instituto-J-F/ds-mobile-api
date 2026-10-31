package br.com.ecociente.mobile.core.service.calendario;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import br.com.ecociente.mobile.core.service.notificacoes.PushService;

import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.calendario.Coleta;
import br.com.ecociente.mobile.core.domain.calendario.NovaColeta;
import br.com.ecociente.mobile.core.domain.calendario.StatusType;
import br.com.ecociente.mobile.core.exception.calendario.AgendamentoInvalidoException;
import br.com.ecociente.mobile.core.exception.calendario.AgendamentoNaoEncontradoException;
import br.com.ecociente.mobile.core.exception.calendario.CondominioNaoEncontradoException;
import br.com.ecociente.mobile.core.exception.calendario.CooperativaNaoEncontradaException;
import br.com.ecociente.mobile.core.exception.calendario.OperacaoColetaNaoPermitidaException;
import br.com.ecociente.mobile.core.gateway.calendario.ColetaGateway;

@Service
public class ColetaService {

  private final ColetaGateway gateway;
  private final PushService push;

  public ColetaService(ColetaGateway gateway, PushService push) {
    this.gateway = gateway;
    this.push = push;
  }

  @Transactional
  public Coleta criar(
      Integer usuarioId,
      NovaColeta entrada) {

    validarUsuario(usuarioId);
    validarEntrada(entrada);

    if (!gateway.condominioAtivo(entrada.condominioId())) {
      throw new CondominioNaoEncontradoException(
          entrada.condominioId());
    }

    if (!gateway.cooperativaAtiva(entrada.cooperativaId())) {
      throw new CooperativaNaoEncontradaException(
          entrada.cooperativaId());
    }

    boolean sindico = gateway.sindicoResponsavel(
        usuarioId,
        entrada.condominioId());

    boolean cooperativa = gateway.responsavelCooperativa(
        usuarioId,
        entrada.cooperativaId());

    if (!sindico && !cooperativa) {
      throw proibido(
          "Você não representa o condomínio nem a cooperativa informados");
    }

    if (sindico && cooperativa) {
      throw proibido(
          "O mesmo usuário não pode representar as duas partes");
    }

    Coleta coleta = gateway.criar(
        usuarioId,
        sindico ? "CONDOMINIO" : "COOPERATIVA",
        entrada);
    push.criar(usuarioId, "calendario", "coleta_criada:" + coleta.id(),
        "Solicitação registrada ♻️", "Sua solicitação de coleta foi registrada.", "calendario", null);
    return coleta;
  }

  @Transactional
  public Coleta atualizarStatus(
      Integer usuarioId,
      Integer coletaId,
      StatusType novoStatus) {

    validarUsuario(usuarioId);

    if (coletaId == null || coletaId <= 0) {
      throw invalido("Informe um ID de agendamento válido");
    }

    if (novoStatus == null) {
      throw invalido("Informe o novo status");
    }

    Coleta coleta = gateway.buscarComBloqueio(coletaId)
        .orElseThrow(
            () -> new AgendamentoNaoEncontradoException(coletaId));

    boolean sindico = gateway.sindicoResponsavel(
        usuarioId,
        coleta.condominioId());

    boolean cooperativa = gateway.responsavelCooperativa(
        usuarioId,
        coleta.cooperativaId());

    if (!sindico && !cooperativa) {
      throw proibido("Você não participa deste agendamento");
    }

    if (novoStatus == StatusType.REALIZADO) {
      if (!cooperativa) {
        throw proibido(
            "Somente a cooperativa envolvida pode marcar como realizado");
      }
    } else {
      validarDestinatario(
          usuarioId,
          coleta,
          sindico,
          cooperativa);
    }

    validarTransicao(coleta.status(), novoStatus);

    Integer respondidoPor =
        novoStatus == StatusType.REALIZADO
            ? coleta.respondidoPorUsuarioId()
            : usuarioId;

    Coleta atualizada = gateway.atualizarStatus(
        coletaId,
        novoStatus,
        respondidoPor);
    String tipo = novoStatus == StatusType.CONFIRMADO ? "solicitacao_aceita"
        : novoStatus == StatusType.RECUSADO ? "solicitacao_recusada" : "calendario";
    push.criar(coleta.solicitadoPorUsuarioId(), tipo,
        "coleta_status:" + coletaId + ":" + novoStatus,
        novoStatus == StatusType.CONFIRMADO ? "Coleta confirmada ✅" : "Atualização da coleta 🚛",
        "A situação da sua coleta foi atualizada.", "calendario", "Ver detalhes");
    return atualizada;
  }

  private void validarDestinatario(
      Integer usuarioId,
      Coleta coleta,
      boolean sindico,
      boolean cooperativa) {

    if (coleta.solicitadoPorUsuarioId() == null
        || coleta.origemSolicitacao() == null) {
      throw invalido(
          "O agendamento não possui identificação completa do solicitante");
    }

    if (usuarioId.equals(coleta.solicitadoPorUsuarioId())) {
      throw proibido(
          "O solicitante não pode responder ou cancelar a própria solicitação");
    }

    boolean destinatario = switch (coleta.origemSolicitacao()) {
      case "CONDOMINIO" -> cooperativa;
      case "COOPERATIVA" -> sindico;
      default -> throw invalido(
          "Origem da solicitação não reconhecida");
    };

    if (!destinatario) {
      throw proibido(
          "Somente a parte destinatária pode responder ou cancelar");
    }
  }

  private void validarTransicao(
      StatusType atual,
      StatusType novo) {

    if (atual == null) {
      throw invalido("O agendamento não possui status válido");
    }

    boolean permitida = switch (atual) {
      case AGENDADO ->
          novo == StatusType.CONFIRMADO
              || novo == StatusType.RECUSADO
              || novo == StatusType.CANCELADO;

      case CONFIRMADO ->
          novo == StatusType.REALIZADO
              || novo == StatusType.CANCELADO;

      case RECUSADO, CANCELADO, REALIZADO -> false;
    };

    if (!permitida) {
      throw invalido(
          "Não é permitido alterar de " + atual + " para " + novo);
    }
  }

  private void validarUsuario(Integer usuarioId) {
    if (usuarioId == null || !gateway.usuarioAtivo(usuarioId)) {
      throw proibido("Usuário não encontrado ou inativo");
    }
  }

  private void validarEntrada(NovaColeta entrada) {
    if (entrada == null
        || entrada.condominioId() == null
        || entrada.condominioId() <= 0
        || entrada.cooperativaId() == null
        || entrada.cooperativaId() <= 0
        || entrada.dataInicio() == null) {
      throw invalido(
          "Informe condomínio, cooperativa e data de início válidos");
    }

    if (!entrada.dataInicio().isAfter(OffsetDateTime.now())) {
      throw invalido("Data de início deve estar no futuro");
    }

    if (entrada.dataFim() != null
        && entrada.dataFim().isBefore(entrada.dataInicio())) {
      throw invalido(
          "Data de fim deve ser igual ou posterior ao início");
    }
  }

  private AgendamentoInvalidoException invalido(String mensagem) {
    return new AgendamentoInvalidoException(mensagem);
  }

  private OperacaoColetaNaoPermitidaException proibido(
      String mensagem) {
    return new OperacaoColetaNaoPermitidaException(mensagem);
  }
}