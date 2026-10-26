package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.ChatService;
import br.com.ecociente.mobile.entrypoint.dto.request.ChatRequests.Arquivamento;
import br.com.ecociente.mobile.entrypoint.dto.request.ChatRequests.Criar;
import br.com.ecociente.mobile.entrypoint.dto.request.ChatRequests.Enviar;
import br.com.ecociente.mobile.entrypoint.dto.request.ChatRequests.Leitura;
import br.com.ecociente.mobile.entrypoint.dto.response.ChatResponses.ConversaDto;
import br.com.ecociente.mobile.entrypoint.dto.response.ChatResponses.LeituraDto;
import br.com.ecociente.mobile.entrypoint.dto.response.ChatResponses.MensagemDto;
import br.com.ecociente.mobile.entrypoint.dto.response.ChatResponses.PaginaDto;
import br.com.ecociente.mobile.entrypoint.mapper.ChatMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/conversas")
public class ChatController {

  private final ChatService service;
  private final ChatMapper mapper;

  public ChatController(
      ChatService service,
      ChatMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @PostMapping
  public ResponseEntity<ConversaDto> criar(
      @AuthenticationPrincipal Integer usuarioId,
      @Valid @RequestBody Criar request) {
    var conversa = service.criar(
        usuarioId,
        request.condominioId(),
        request.cooperativaId());

    return ResponseEntity.ok(
        mapper.conversa(conversa));
  }

  @GetMapping
  public ResponseEntity<PaginaDto<ConversaDto>> listar(
      @AuthenticationPrincipal Integer usuarioId,
      @RequestParam(name = "arquivadas", defaultValue = "false") boolean arquivadas,
      @RequestParam(name = "page", defaultValue = "0") int pagina,
      @RequestParam(name = "size", defaultValue = "20") int tamanho) {
    return ResponseEntity.ok(
        mapper.conversas(
            service.listar(
                usuarioId,
                arquivadas,
                pagina,
                tamanho)));
  }

  @PostMapping("/{id}/mensagens")
  public ResponseEntity<MensagemDto> enviar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer conversaId,
      @Valid @RequestBody Enviar request) {
    var mensagem = service.enviar(
        usuarioId,
        conversaId,
        request.conteudo());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.mensagem(mensagem));
  }

  @GetMapping("/{id}/mensagens")
  public ResponseEntity<PaginaDto<MensagemDto>> mensagens(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer conversaId,
      @RequestParam(name = "page", defaultValue = "0") int pagina,
      @RequestParam(name = "size", defaultValue = "20") int tamanho) {
    return ResponseEntity.ok(
        mapper.mensagens(
            service.mensagens(
                usuarioId,
                conversaId,
                pagina,
                tamanho)));
  }

  @PatchMapping("/{id}/leitura")
  public ResponseEntity<LeituraDto> ler(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer conversaId,
      @Valid @RequestBody(required = false) Leitura request) {
    int quantidade = service.ler(
        usuarioId,
        conversaId,
        request == null
            ? null
            : request.ateMensagemId());

    return ResponseEntity.ok(
        new LeituraDto(quantidade));
  }

  @PatchMapping("/{id}/arquivamento")
  public ResponseEntity<Void> arquivar(
      @AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer conversaId,
      @Valid @RequestBody Arquivamento request) {
    service.arquivar(
        usuarioId,
        conversaId,
        request.arquivada());

    return ResponseEntity.noContent().build();
  }
}