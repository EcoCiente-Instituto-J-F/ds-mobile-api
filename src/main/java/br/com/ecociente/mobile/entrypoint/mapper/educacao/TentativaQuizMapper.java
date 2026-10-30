package br.com.ecociente.mobile.entrypoint.mapper.educacao;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.educacao.RespostaQuiz;
import br.com.ecociente.mobile.core.domain.educacao.ResultadoFinalizacaoQuiz;
import br.com.ecociente.mobile.core.domain.educacao.RevisaoTentativaQuiz;
import br.com.ecociente.mobile.core.domain.educacao.ResumoTentativaQuiz;
import br.com.ecociente.mobile.core.domain.educacao.TentativaQuizIniciada;
import br.com.ecociente.mobile.entrypoint.dto.educacao.request.FinalizarTentativaQuizRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.educacao.response.ResultadoTentativaQuizResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.educacao.response.ResultadoTentativaQuizResponseDto.PontuacaoResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.educacao.response.ResumoTentativaQuizResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.educacao.response.RevisaoTentativaQuizResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.educacao.response.RevisaoTentativaQuizResponseDto.AlternativaRevisaoResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.educacao.response.RevisaoTentativaQuizResponseDto.PerguntaRevisaoResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.educacao.response.TentativaQuizIniciadaResponseDto;

@Component
public class TentativaQuizMapper {

    private final QuizMapper quizMapper;

    public TentativaQuizMapper(QuizMapper quizMapper) {
        this.quizMapper = quizMapper;
    }

    public TentativaQuizIniciadaResponseDto toResponse(
            TentativaQuizIniciada tentativa) {
        return new TentativaQuizIniciadaResponseDto(
                tentativa.tentativaId(),
                tentativa.iniciadoEm(),
                quizMapper.toResponse(tentativa.quiz()));
    }

    public List<RespostaQuiz> toDomain(
            FinalizarTentativaQuizRequestDto request) {
        return request.respostas().stream()
                .map(resposta -> new RespostaQuiz(
                        resposta.perguntaId(),
                        resposta.alternativaId()))
                .toList();
    }

    public ResultadoTentativaQuizResponseDto toResponse(
            ResultadoFinalizacaoQuiz resultado) {
        var tentativa = resultado.tentativa();
        var pontuacao = resultado.pontuacao();

        return new ResultadoTentativaQuizResponseDto(
                tentativa.tentativaId(),
                tentativa.quizId(),
                tentativa.nota().movePointLeft(1),
                tentativa.totalPerguntas(),
                tentativa.acertos(),
                tentativa.aprovado(),
                tentativa.concluidoEm(),
                new PontuacaoResponseDto(
                        pontuacao.status().name(),
                        pontuacao.pontos(),
                        pontuacao.movimentacaoId(),
                        pontuacao.rankingSincronizado()));
    }

    public RevisaoTentativaQuizResponseDto toResponse(
            RevisaoTentativaQuiz revisao) {
        var resultado = revisao.resultado();

        var perguntas = revisao.perguntas().stream()
                .map(item -> new PerguntaRevisaoResponseDto(
                        item.perguntaId(),
                        item.enunciado(),
                        item.ordem(),
                        new AlternativaRevisaoResponseDto(
                                item.escolhida().id(),
                                item.escolhida().texto()),
                        new AlternativaRevisaoResponseDto(
                                item.correta().id(),
                                item.correta().texto()),
                        item.acertouNaTentativa(),
                        null))
                .toList();

        return new RevisaoTentativaQuizResponseDto(
                resultado.tentativaId(),
                resultado.quizId(),
                resultado.nota().movePointLeft(1),
                resultado.totalPerguntas(),
                resultado.acertos(),
                resultado.aprovado(),
                resultado.concluidoEm(),
                true,
                perguntas);
    }

    public ResumoTentativaQuizResponseDto toResponse(
            ResumoTentativaQuiz resumo) {
        return new ResumoTentativaQuizResponseDto(
                resumo.tentativaId(),
                resumo.quizId(),
                resumo.titulo(),
                resumo.nota(),
                resumo.aprovado(),
                resumo.concluidoEm());
    }
}
