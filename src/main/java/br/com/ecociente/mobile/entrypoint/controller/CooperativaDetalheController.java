package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.ConsultarCooperativaService;
import br.com.ecociente.mobile.entrypoint.dto.response.CooperativaResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.CooperativaMapper;

@RestController
public class CooperativaDetalheController {

  private final ConsultarCooperativaService service;
  private final CooperativaMapper mapper;

  public CooperativaDetalheController(
      ConsultarCooperativaService service,
      CooperativaMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping("/api/cooperativas/{id}")
  public ResponseEntity<CooperativaResponseDto> consultar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer cooperativaId) {

    var cooperativa = service.executar(
        usuarioId,
        cooperativaId);

    return ResponseEntity.ok(
        mapper.toResponse(cooperativa));
  }
}