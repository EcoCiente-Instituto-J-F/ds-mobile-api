package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.DashboardSindico;
import br.com.ecociente.mobile.entrypoint.dto.response.DashboardSindicoResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.DashboardSindicoResponseDto.MaterialPorCategoriaDto;
import br.com.ecociente.mobile.entrypoint.dto.response.DashboardSindicoResponseDto.MateriaisDto;
import br.com.ecociente.mobile.entrypoint.dto.response.DashboardSindicoResponseDto.NotaPorQuizDto;
import br.com.ecociente.mobile.entrypoint.dto.response.DashboardSindicoResponseDto.PontosDto;
import br.com.ecociente.mobile.entrypoint.dto.response.DashboardSindicoResponseDto.QuizzesDto;

@Component
public class DashboardSindicoMapper {

  public DashboardSindicoResponseDto toResponse(
      DashboardSindico dashboard) {
    var materiais = dashboard.materiais();
    var pontos = dashboard.pontos();
    var quizzes = dashboard.quizzes();

    return new DashboardSindicoResponseDto(
        dashboard.condominioId(),
        dashboard.nomeCondominio(),
        dashboard.nomeSindico(),
        dashboard.totalMoradores(),
        new MateriaisDto(
            materiais.reciclados(),
            materiais.emValidacao(),
            materiais.aguardandoSindico(),
            materiais.reprovadas(),
            materiais.porCategoria().stream()
                .map(item -> new MaterialPorCategoriaDto(
                    item.categoriaId(),
                    item.categoria(),
                    item.cor(),
                    item.quantidade()))
                .toList()),
        new PontosDto(
            pontos.total(),
            pontos.postagens(),
            pontos.quizzes(),
            pontos.mesAtual()),
        new QuizzesDto(
            quizzes.tentativasConcluidas(),
            quizzes.tentativasAprovadas(),
            quizzes.moradoresComQuiz(),
            quizzes.notaMedia(),
            quizzes.taxaAprovacao(),
            quizzes.porQuiz().stream()
                .map(item -> new NotaPorQuizDto(
                    item.quizId(),
                    item.quiz(),
                    item.tentativas(),
                    item.aprovadas(),
                    item.notaMedia()))
                .toList()));
  }
}
