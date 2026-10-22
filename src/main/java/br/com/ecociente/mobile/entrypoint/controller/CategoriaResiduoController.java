package br.com.ecociente.mobile.entrypoint.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.ConsultarCategoriasResiduoService;
import br.com.ecociente.mobile.entrypoint.dto.response.CategoriaResiduoResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.PostagemMapper;

@RestController
public class CategoriaResiduoController {

  private final ConsultarCategoriasResiduoService service;
  private final PostagemMapper mapper;

  public CategoriaResiduoController(
      ConsultarCategoriasResiduoService service,
      PostagemMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping("/api/categorias-residuos")
  public ResponseEntity<List<CategoriaResiduoResponseDto>> listar() {
    return ResponseEntity.ok(
        service.executar().stream()
            .map(mapper::toResponse)
            .toList());
  }
}
