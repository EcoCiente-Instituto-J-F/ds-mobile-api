package br.com.ecociente.mobile.core.service.pontuacao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.pontuacao.MovimentacaoPontos;
import br.com.ecociente.mobile.core.exception.pontuacao.BusinessException;
import br.com.ecociente.mobile.core.exception.pontuacao.NotFoundException;
import br.com.ecociente.mobile.core.gateway.pontuacao.TentativaQuizGateway;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConsolidarQuizInternoService {

  private final TentativaQuizGateway tentativaGateway;
  private final PontuacaoService pontuacaoService;

  @Transactional
  public MovimentacaoPontos executar(
      Integer tentativaId,
      Integer usuarioId) {

    if (tentativaId == null || tentativaId <= 0
        || usuarioId == null || usuarioId <= 0) {
      throw new BusinessException(
          "IDENTIFICADOR_INVALIDO",
          "Tentativa e usuário devem ser válidos");
    }

    var tentativa = tentativaGateway
        .buscarComBloqueio(tentativaId)
        .orElseThrow(() -> new NotFoundException(
            "TENTATIVA_NAO_ENCONTRADA",
            "Tentativa não encontrada"));

    if (!usuarioId.equals(tentativa.getUsuarioId())) {
      throw new BusinessException(
          "TENTATIVA_NAO_PERTENCE_AO_USUARIO",
          "A tentativa não pertence ao usuário");
    }

    return pontuacaoService.consolidarQuiz(
        tentativaId,
        "quiz-tentativa-" + tentativaId);
  }
}