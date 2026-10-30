package br.com.ecociente.mobile.entrypoint.controller.validador;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.validador.ValidadorPostagemValidacaoService;
import br.com.ecociente.mobile.entrypoint.dto.validador.request.DecisaoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/v1/postagens")
@Validated
public class DecisaoController {


  private final ValidadorPostagemValidacaoService service;

  public DecisaoController(ValidadorPostagemValidacaoService service) {
    this.service = service;
  }

  @PostMapping("/{postagemId}/decisao")
  public ResponseEntity<Void> decidir(
      @PathVariable @Min(1) int postagemId,
      org.springframework.security.core.Authentication authentication,
      @Valid @RequestBody DecisaoRequest decisao) {
    service.decidir(postagemId, br.com.ecociente.mobile.entrypoint.security.ValidadorUsuario.id(authentication, decisao.usuarioId()), decisao.aprovar());
    return ResponseEntity.noContent().build();
  }
}
