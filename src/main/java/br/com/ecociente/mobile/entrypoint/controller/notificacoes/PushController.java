package br.com.ecociente.mobile.entrypoint.controller.notificacoes;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ecociente.mobile.core.service.notificacoes.PushService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notificacoes")
public class PushController {

    private final PushService push;
    private final ObjectMapper mapper;

    public record RegistrarDispositivoRequest(
            @NotBlank String instalacaoId,
            @NotBlank @Size(max = 1024) String token,
            Map<String, Boolean> preferencias) {
    }

    @PostMapping("/dispositivos")
    public ResponseEntity<Void> registrar(
            @AuthenticationPrincipal Integer usuarioId,
            @Valid @RequestBody RegistrarDispositivoRequest request) throws JsonProcessingException {
        Map<String, Boolean> preferencias = request.preferencias() == null ? Map.of() : request.preferencias();
        push.registrarDispositivo(usuarioId, request.instalacaoId(), request.token(),
                mapper.writeValueAsString(preferencias));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/dispositivos/{instalacaoId}")
    public ResponseEntity<Void> desativar(
            @AuthenticationPrincipal Integer usuarioId,
            @PathVariable String instalacaoId) {
        push.desativarDispositivo(usuarioId, instalacaoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<Map<String, Object>> historico(
            @AuthenticationPrincipal Integer usuarioId,
            @RequestParam(defaultValue = "50") int limite) {
        return push.historico(usuarioId, limite);
    }

    @PatchMapping("/{notificacaoId}/lida")
    public ResponseEntity<Void> marcarLida(
            @AuthenticationPrincipal Integer usuarioId,
            @PathVariable int notificacaoId) {
        push.marcarLida(usuarioId, notificacaoId);
        return ResponseEntity.noContent().build();
    }
}
