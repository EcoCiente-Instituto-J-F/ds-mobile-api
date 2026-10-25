package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.ConsultarPostagensEmAnaliseService;
import br.com.ecociente.mobile.entrypoint.dto.response.PostagensResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.PostagemMapper;

@RestController
public class PostagensEmAnaliseController {

  private final ConsultarPostagensEmAnaliseService service;
  private final PostagemMapper mapper;

  public PostagensEmAnaliseController(
      ConsultarPostagensEmAnaliseService service,
      PostagemMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping("/api/condominios/{id}/postagens/em-analise")
  public ResponseEntity<PostagensResponseDto> consultar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer condominioId,
      @RequestParam(name = "page", defaultValue = "0") int pagina,
      @RequestParam(name = "size", defaultValue = "10") int tamanho) {

    var resultado = service.executar(
        usuarioId,
        condominioId,
        pagina,
        tamanho);

    return ResponseEntity.ok(
        mapper.toResponse(resultado));
  }
}