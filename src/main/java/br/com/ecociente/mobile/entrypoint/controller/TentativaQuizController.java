package br.com.ecociente.mobile.entrypoint.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import br.com.ecociente.mobile.core.service.ConsultarRevisaoQuizService;
import br.com.ecociente.mobile.core.service.FinalizarQuizComPontuacaoService;
import br.com.ecociente.mobile.core.service.IniciarTentativaQuizService;
import br.com.ecociente.mobile.entrypoint.dto.request.FinalizarTentativaQuizRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.response.ResultadoTentativaQuizResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.ResumoTentativaQuizResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.RevisaoTentativaQuizResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.TentativaQuizIniciadaResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.TentativaQuizMapper;
import jakarta.validation.Valid;

@RestController
public class TentativaQuizController {

  private final IniciarTentativaQuizService iniciarService;
  private final FinalizarQuizComPontuacaoService finalizarService;
  private final ConsultarRevisaoQuizService revisaoService;
  private final TentativaQuizMapper mapper;

  public TentativaQuizController(
      IniciarTentativaQuizService iniciarService,
      FinalizarQuizComPontuacaoService finalizarService,
      ConsultarRevisaoQuizService revisaoService,
      TentativaQuizMapper mapper) {
    this.iniciarService = iniciarService;
    this.finalizarService = finalizarService;
    this.revisaoService = revisaoService;
    this.mapper = mapper;
  }

  @PostMapping("/api/quizzes/{id}/tentativas")
  public ResponseEntity<TentativaQuizIniciadaResponseDto> iniciar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer quizId) {
    var tentativa = iniciarService.executar(
        usuarioId,
        quizId);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(mapper.toResponse(tentativa));
  }

  @PostMapping("/api/tentativas-quiz/{id}/finalizar")
  public ResponseEntity<ResultadoTentativaQuizResponseDto> finalizar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer tentativaId,
      @Valid @RequestBody FinalizarTentativaQuizRequestDto request) {
    var resultado = finalizarService.executar(
        usuarioId,
        tentativaId,
        mapper.toDomain(request));

    return ResponseEntity.ok(
        mapper.toResponse(resultado));
  }

  @GetMapping("/api/tentativas-quiz/{id}/revisao")
  public ResponseEntity<RevisaoTentativaQuizResponseDto> revisar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer tentativaId) {
    var revisao = revisaoService.executar(
        usuarioId,
        tentativaId);

    return ResponseEntity.ok(
        mapper.toResponse(revisao));
  }

  @GetMapping("/api/tentativas-quiz")
  public ResponseEntity<List<ResumoTentativaQuizResponseDto>> historico(
      @AuthenticationPrincipal Integer usuarioId,
      @RequestParam(name = "limite", defaultValue = "20") int limite) {

    var historico = revisaoService.historico(
        usuarioId,
        limite);

    return ResponseEntity.ok(
        historico.stream()
            .map(item -> mapper.toResponse(item))
            .toList());
  }

  @GetMapping("/api/tentativas-quiz/quizzes-concluidos")
  public ResponseEntity<List<Integer>> quizzesConcluidos(
      @AuthenticationPrincipal Integer usuarioId) {

    return ResponseEntity.ok(
        revisaoService.quizzesConcluidos(usuarioId));
  }
}
