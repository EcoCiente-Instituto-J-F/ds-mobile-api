package br.com.ecociente.mobile.entrypoint.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.AulaService;
import br.com.ecociente.mobile.entrypoint.dto.response.AulaResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.AulaMapper;

@RestController
public class AulaController {

    private final AulaService service;
    private final AulaMapper mapper;

    public AulaController(
        AulaService service,
        AulaMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/api/cursos/{cursoId}/aulas")
    public ResponseEntity<List<AulaResponseDto>> listarAulas(
        @PathVariable("cursoId") Integer cursoId
    ) {
        return service.listarAulas(cursoId)
            .map(aulas -> aulas.stream()
                .map(mapper::toResponse)
                .toList())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/aulas/{aulaId}")
    public ResponseEntity<AulaResponseDto> buscarAula(
        @PathVariable("aulaId") Integer aulaId
    ) {
        return service.buscarAula(aulaId)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}