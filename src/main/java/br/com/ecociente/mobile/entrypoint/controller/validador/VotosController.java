package br.com.ecociente.mobile.entrypoint.controller.validador;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.domain.validador.ResultadoVoto;
import br.com.ecociente.mobile.core.service.validador.ValidadorPostagemValidacaoService;
import br.com.ecociente.mobile.entrypoint.dto.validador.request.VotoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/v1/postagens")
@Validated
public class VotosController {


  private final ValidadorPostagemValidacaoService service;

  public VotosController(ValidadorPostagemValidacaoService service) {
    this.service = service;
  }

  @PostMapping("/{postagemId}/votos")
  public ResponseEntity<ResultadoVoto> votar(
      @PathVariable @Min(1) int postagemId,
      org.springframework.security.core.Authentication authentication,
      @Valid @RequestBody VotoRequest voto) {
    var resultado = service.votar(
        postagemId, br.com.ecociente.mobile.entrypoint.security.ValidadorUsuario.id(authentication, voto.usuarioId()), voto.tipo(), voto.motivoDenunciaId(), voto.comentario());
    return ResponseEntity.ok(resultado);
  }
}
