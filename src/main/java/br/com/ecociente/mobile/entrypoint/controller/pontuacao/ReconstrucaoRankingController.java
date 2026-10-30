package br.com.ecociente.mobile.entrypoint.controller.pontuacao;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.pontuacao.ReconstrucaoRankingService;
import br.com.ecociente.mobile.core.service.pontuacao.ReconstrucaoRankingService.ResultadoReconstrucao;
import br.com.ecociente.mobile.entrypoint.dto.pontuacao.request.ReconstruirRankingRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/rankings")
public class ReconstrucaoRankingController {

    private final ReconstrucaoRankingService service;

    public ReconstrucaoRankingController(
        ReconstrucaoRankingService service
    ) {
        this.service = service;
    }

    @PostMapping("/reconstruir")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResultadoReconstrucao> reconstruir(
        @Valid @RequestBody ReconstruirRankingRequest request
    ) {
        return ResponseEntity.ok(
            service.reconstruir(
                request.tipo(),
                request.condominioId()
            )
        );
    }
}