package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.ConsultarDashboardSindicoService;
import br.com.ecociente.mobile.entrypoint.dto.response.DashboardSindicoResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.DashboardSindicoMapper;

@RestController
public class DashboardSindicoController {

  private final ConsultarDashboardSindicoService service;
  private final DashboardSindicoMapper mapper;

  public DashboardSindicoController(
      ConsultarDashboardSindicoService service,
      DashboardSindicoMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping("/api/condominios/{id}/dashboard")
  public ResponseEntity<DashboardSindicoResponseDto> consultar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer condominioId) {
    var dashboard = service.executar(
        usuarioId,
        condominioId);

    return ResponseEntity.ok(
        mapper.toResponse(dashboard));
  }
}
