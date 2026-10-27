package br.com.ecociente.mobile.entrypoint.controller;

import br.com.ecociente.mobile.core.service.MensagemMotivacionalService;
import br.com.ecociente.mobile.core.service.MensagemMotivacionalService.MensagemMotivacional;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notificacoes")
public class MensagemMotivacionalController {

    private final MensagemMotivacionalService service;

    public MensagemMotivacionalController(MensagemMotivacionalService service) {
        this.service = service;
    }

    @GetMapping("/motivacional")
    public ResponseEntity<MensagemMotivacional> buscar(
            @RequestParam(required = false) Long excluirId) {
        return service.sortear(excluirId)
                .map(mensagem -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(mensagem))
                .orElseGet(() -> ResponseEntity.notFound()
                        .cacheControl(CacheControl.noStore())
                        .build());
    }
}
