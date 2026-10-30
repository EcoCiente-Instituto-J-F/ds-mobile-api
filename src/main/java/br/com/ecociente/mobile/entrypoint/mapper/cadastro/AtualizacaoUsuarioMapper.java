package br.com.ecociente.mobile.entrypoint.mapper.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.AtualizacaoCondominio;
import br.com.ecociente.mobile.core.domain.cadastro.AtualizacaoDados;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.request.AtualizarCondominioRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.request.AtualizarUsuarioRequestDto;

@Component
public class AtualizacaoUsuarioMapper {

    public AtualizacaoDados toDomain(AtualizarUsuarioRequestDto request) {
        return new AtualizacaoDados(
            request.nome(),
            request.email(),
            request.dataNascimento(),
            request.urlAvatar(),
            request.telefone(),
            request.telefoneId()
        );
    }

    public AtualizacaoCondominio toDomain(AtualizarCondominioRequestDto request) {
        return new AtualizacaoCondominio(
            request.nome(),
            request.tipoCondominioId(),
            request.urlFoto()
        );
    }
}