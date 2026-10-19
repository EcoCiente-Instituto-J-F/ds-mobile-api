package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.ConsultarAvaliacoesCooperativaService;
import br.com.ecociente.mobile.entrypoint.dto.response.AvaliacoesCooperativaResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.AvaliacoesCooperativaMapper;

@RestController
public class AvaliacoesCooperativaController {

  private final ConsultarAvaliacoesCooperativaService service;
  private final AvaliacoesCooperativaMapper mapper;

  public AvaliacoesCooperativaController(
      ConsultarAvaliacoesCooperativaService service,
      AvaliacoesCooperativaMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping("/api/cooperativas/{id}/avaliacoes")
  public ResponseEntity<AvaliacoesCooperativaResponseDto> consultar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer cooperativaId,
      @RequestParam(name = "page", defaultValue = "0") int pagina,
      @RequestParam(name = "size", defaultValue = "10") int tamanho) {
    var resultado = service.executar(
        usuarioId,
        cooperativaId,
        pagina,
        tamanho);

    return ResponseEntity.ok(
        mapper.toResponse(resultado));
  }
}