package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import br.com.ecociente.mobile.core.service.ConsultarCondominiosAtendidosService;
import br.com.ecociente.mobile.entrypoint.dto.response.CondominiosAtendidosResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.CondominiosAtendidosMapper;

@RestController
public class CondominiosAtendidosController {
  private final ConsultarCondominiosAtendidosService service;
  private final CondominiosAtendidosMapper mapper;
  public CondominiosAtendidosController(ConsultarCondominiosAtendidosService service,
      CondominiosAtendidosMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }
  @GetMapping("/api/cooperativas/{id}/condominios")
  public ResponseEntity<CondominiosAtendidosResponseDto> consultar(
      @AuthenticationPrincipal Integer usuarioId, @PathVariable("id") Integer cooperativaId,
      @RequestParam(name = "page", defaultValue = "0") int pagina,
      @RequestParam(name = "size", defaultValue = "10") int tamanho) {
    return ResponseEntity.ok(mapper.toResponse(service.executar(usuarioId, cooperativaId, pagina, tamanho)));
  }
}
