package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.ConsultarCooperativasProximasService;
import br.com.ecociente.mobile.entrypoint.dto.response.CooperativasResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.CooperativaMapper;

@RestController
public class CooperativaController {

  private final ConsultarCooperativasProximasService service;
  private final CooperativaMapper mapper;

  public CooperativaController(
      ConsultarCooperativasProximasService service,
      CooperativaMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping("/api/cooperativas")
  public ResponseEntity<CooperativasResponseDto> listarProximas(
      @AuthenticationPrincipal Integer usuarioId,
      @RequestParam(name = "page", defaultValue = "0") int pagina,
      @RequestParam(name = "size", defaultValue = "10") int tamanho) {
    var resultado = service.executar(
        usuarioId,
        pagina,
        tamanho);

    return ResponseEntity.ok(
        mapper.toResponse(resultado));
  }
}
