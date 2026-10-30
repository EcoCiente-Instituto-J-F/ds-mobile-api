package br.com.ecociente.mobile.entrypoint.controller.calendario;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import br.com.ecociente.mobile.config.security.CalendarioUsuario;
import br.com.ecociente.mobile.core.service.calendario.ColetaService;
import br.com.ecociente.mobile.entrypoint.dto.calendario.request.AtualizarStatusColetaRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.calendario.request.CriarColetaRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.calendario.response.ColetaResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.calendario.ColetaMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/agendamentos")
public class ColetaController {

  private final ColetaService service;
  private final ColetaMapper mapper;

  @PostMapping
  public ResponseEntity<ColetaResponseDto> criar(
      Authentication authentication,
      @Valid @RequestBody CriarColetaRequestDto request) {

    var coleta = service.criar(
        CalendarioUsuario.id(authentication),
        mapper.toDomain(request));

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(mapper.toResponse(coleta));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<ColetaResponseDto> atualizarStatus(
      Authentication authentication,
      @PathVariable("id") Integer coletaId,
      @Valid @RequestBody AtualizarStatusColetaRequestDto request) {

    var coleta = service.atualizarStatus(
        CalendarioUsuario.id(authentication),
        coletaId,
        request.status());

    return ResponseEntity.ok(mapper.toResponse(coleta));
  }
}