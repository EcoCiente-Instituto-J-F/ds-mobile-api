package br.com.ecociente.mobile.entrypoint.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.CursoService;
import br.com.ecociente.mobile.entrypoint.dto.response.CursoResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.CursoMapper;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    private final CursoService service;
    private final CursoMapper mapper;

    public CursoController(
        CursoService service,
        CursoMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<CursoResponseDto>> listarCursos() {
        List<CursoResponseDto> cursos = service.listarCursos()
            .stream()
            .map(mapper::toResponse)
            .toList();

        return ResponseEntity.ok(cursos);
    }

    @GetMapping("/{cursoId}")
    public ResponseEntity<CursoResponseDto> buscarCurso(
        @PathVariable("cursoId") Integer cursoId
    ) {
        return service.buscarCurso(cursoId)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}