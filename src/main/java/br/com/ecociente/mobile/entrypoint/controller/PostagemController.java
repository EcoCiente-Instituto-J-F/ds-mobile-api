package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.ConsultarPostagensCondominioService;
import br.com.ecociente.mobile.core.service.ConsultarPostagensUsuarioService;
import br.com.ecociente.mobile.core.service.CriarPostagemService;
import br.com.ecociente.mobile.core.service.VotarPostagemService;
import br.com.ecociente.mobile.entrypoint.dto.request.CriarPostagemRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.request.VotarPostagemRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.response.PostagemResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.PostagensResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.ResultadoVotoResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.PostagemMapper;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/postagens")
public class PostagemController {

  private final CriarPostagemService criarService;
  private final ConsultarPostagensCondominioService condominioService;
  private final ConsultarPostagensUsuarioService usuarioService;
  private final VotarPostagemService votarService;
  private final PostagemMapper mapper;

  public PostagemController(
      CriarPostagemService criarService,
      ConsultarPostagensCondominioService condominioService,
      ConsultarPostagensUsuarioService usuarioService,
      VotarPostagemService votarService,
      PostagemMapper mapper) {
    this.criarService = criarService;
    this.condominioService = condominioService;
    this.usuarioService = usuarioService;
    this.votarService = votarService;
    this.mapper = mapper;
  }

  @PostMapping
  public ResponseEntity<PostagemResponseDto> criar(
      @AuthenticationPrincipal Integer usuarioId,
      @Valid @RequestBody CriarPostagemRequestDto request) {
    var postagem = criarService.executar(
        usuarioId,
        request.condominioId(),
        request.categoriaId(),
        request.urlFoto(),
        request.hashFoto(),
        request.capturadaEm());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(mapper.toResponse(postagem));
  }

  @PostMapping("/{postagemId}/votos")
  public ResponseEntity<ResultadoVotoResponseDto> votar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("postagemId") Integer postagemId,
      @Valid @RequestBody VotarPostagemRequestDto request) {
    var resultado = votarService.executar(
        usuarioId,
        postagemId,
        request.tipo(),
        request.motivoDenunciaId(),
        request.comentario());

    return ResponseEntity.ok(mapper.toResponse(resultado));
  }

  @GetMapping
  public ResponseEntity<PostagensResponseDto> listarDoCondominio(
      @AuthenticationPrincipal Integer usuarioId,
      @RequestParam("condominioId") Integer condominioId,
      @RequestParam(name = "page", defaultValue = "0") int pagina,
      @RequestParam(name = "size", defaultValue = "10") int tamanho) {
    var resultado = condominioService.executar(
        usuarioId,
        condominioId,
        pagina,
        tamanho);

    return ResponseEntity.ok(
        mapper.toResponse(resultado));
  }

  @GetMapping("/minhas")
  public ResponseEntity<PostagensResponseDto> listarMinhas(
      @AuthenticationPrincipal Integer usuarioId,
      @RequestParam(name = "page", defaultValue = "0") int pagina,
      @RequestParam(name = "size", defaultValue = "10") int tamanho) {
    var resultado = usuarioService.executar(
        usuarioId,
        pagina,
        tamanho);

    return ResponseEntity.ok(
        mapper.toResponse(resultado));
  }
}
