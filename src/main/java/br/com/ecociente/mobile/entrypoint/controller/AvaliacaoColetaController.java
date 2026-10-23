package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.AvaliarColetaService;
import br.com.ecociente.mobile.entrypoint.dto.request.AvaliarColetaRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.response.AvaliacaoColetaResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.AvaliacaoColetaMapper;
import jakarta.validation.Valid;

@RestController
public class AvaliacaoColetaController {

  private final AvaliarColetaService service;
  private final AvaliacaoColetaMapper mapper;

  public AvaliacaoColetaController(
      AvaliarColetaService service,
      AvaliacaoColetaMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @PostMapping("/api/agendamentos/{id}/avaliacao")
  public ResponseEntity<AvaliacaoColetaResponseDto> avaliar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer agendamentoId,
      @Valid @RequestBody AvaliarColetaRequestDto request) {
    var avaliacao = service.executar(
        usuarioId,
        agendamentoId,
        request.nota(),
        request.comentario());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(mapper.toResponse(avaliacao));
  }
}
