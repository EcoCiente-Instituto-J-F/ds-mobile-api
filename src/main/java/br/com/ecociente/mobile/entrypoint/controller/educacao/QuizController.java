package br.com.ecociente.mobile.entrypoint.controller.educacao;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.educacao.QuizService;
import br.com.ecociente.mobile.entrypoint.dto.educacao.response.QuizResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.educacao.QuizMapper;

@RestController
public class QuizController {

    private final QuizService service;
    private final QuizMapper mapper;

    public QuizController(
        QuizService service,
        QuizMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/api/aulas/{aulaId}/quiz")
    public ResponseEntity<QuizResponseDto> buscarQuizDaAula(
        @PathVariable("aulaId") Integer aulaId
    ) {
        return service.buscarQuizDaAula(aulaId)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}