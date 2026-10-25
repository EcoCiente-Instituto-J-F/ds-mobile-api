package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import br.com.ecociente.mobile.core.service.ChatService;
import br.com.ecociente.mobile.entrypoint.dto.request.ChatRequests.*;
import br.com.ecociente.mobile.entrypoint.dto.response.ChatResponses.*;
import br.com.ecociente.mobile.entrypoint.mapper.ChatMapper;

@RestController
@RequestMapping("/api/conversas")
public class ChatController {
  private final ChatService service;
  private final ChatMapper mapper;
  public ChatController(ChatService service, ChatMapper mapper) { this.service = service; this.mapper = mapper; }

  @GetMapping
  public ResponseEntity<PaginaDto<ConversaDto>> listar(@AuthenticationPrincipal Integer usuarioId,
      @RequestParam(name = "arquivadas", defaultValue = "false") boolean arquivadas,
      @RequestParam(name = "page", defaultValue = "0") int pagina,
      @RequestParam(name = "size", defaultValue = "20") int tamanho) {
    return ResponseEntity.ok(mapper.conversas(service.listar(usuarioId, arquivadas, pagina, tamanho)));
  }
  @PostMapping
  public ResponseEntity<ConversaDto> criar(@AuthenticationPrincipal Integer usuarioId, @Valid @RequestBody Criar request) {
    return ResponseEntity.ok(mapper.conversa(service.criar(usuarioId, request.condominioId(), request.cooperativaId())));
  }
  @GetMapping("/{id}/mensagens")
  public ResponseEntity<PaginaDto<MensagemDto>> mensagens(@AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer id, @RequestParam(name = "page", defaultValue = "0") int pagina,
      @RequestParam(name = "size", defaultValue = "20") int tamanho) {
    return ResponseEntity.ok(mapper.mensagens(service.mensagens(usuarioId, id, pagina, tamanho)));
  }
  @PostMapping("/{id}/mensagens")
  public ResponseEntity<MensagemDto> enviar(@AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer id, @Valid @RequestBody Enviar request) {
    return ResponseEntity.status(201).body(mapper.mensagem(service.enviar(usuarioId, id, request.conteudo())));
  }
  @PatchMapping("/{id}/leitura")
  public ResponseEntity<LeituraDto> ler(@AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer id, @Valid @RequestBody(required = false) Leitura request) {
    return ResponseEntity.ok(new LeituraDto(service.ler(usuarioId, id, request == null ? null : request.ateMensagemId())));
  }
  @PatchMapping("/{id}/arquivamento")
  public ResponseEntity<Void> arquivar(@AuthenticationPrincipal Integer usuarioId,
      @PathVariable("id") Integer id, @Valid @RequestBody Arquivamento request) {
    service.arquivar(usuarioId, id, request.arquivada());
    return ResponseEntity.noContent().build();
  }
}
