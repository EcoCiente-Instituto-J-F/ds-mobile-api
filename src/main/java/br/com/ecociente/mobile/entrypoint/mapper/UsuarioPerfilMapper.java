package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.UsuarioPerfil;
import br.com.ecociente.mobile.entrypoint.dto.response.UsuarioPerfilResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.UsuarioPerfilResponseDto.*;

@Component
public class UsuarioPerfilMapper {

    public UsuarioPerfilResponseDto toResponse(UsuarioPerfil usuario) {
        return new UsuarioPerfilResponseDto(
            usuario.id(),
            usuario.nome(),
            usuario.email(),
            usuario.dataNascimento(),
            usuario.cpf(),
            usuario.urlAvatar(),
            usuario.ativo(),
            usuario.registroEm(),
            usuario.tipoUsuarioId(),
            usuario.tipoUsuario(),
            usuario.moradorId(),
            usuario.sindicoId(),
            endereco(usuario.endereco()),
            usuario.telefones().stream()
                .map(telefone -> new TelefoneDto(
                    telefone.id(),
                    telefone.numero(),
                    telefone.tipo(),
                    telefone.ativo()
                ))
                .toList(),
            cooperativa(usuario.cooperativa()),
            usuario.condominios().stream()
                .map(this::condominio)
                .toList()
        );
    }

    private EnderecoDto endereco(UsuarioPerfil.Endereco endereco) {
        if (endereco == null) {
            return null;
        }

        return new EnderecoDto(
            endereco.id(),
            endereco.cep(),
            endereco.estado(),
            endereco.cidade(),
            endereco.logradouro(),
            endereco.numero(),
            endereco.complemento()
        );
    }

    private CooperativaDto cooperativa(
        UsuarioPerfil.Cooperativa cooperativa
    ) {
        if (cooperativa == null) {
            return null;
        }

        return new CooperativaDto(
            cooperativa.id(),
            cooperativa.nome(),
            cooperativa.cnpj(),
            cooperativa.email(),
            cooperativa.telefone(),
            cooperativa.dataCadastro(),
            endereco(cooperativa.endereco())
        );
    }

    private CondominioDto condominio(
        UsuarioPerfil.Condominio condominio
    ) {
        return new CondominioDto(
            condominio.id(),
            condominio.nome(),
            condominio.cnpj(),
            condominio.ativo(),
            condominio.tipoCondominioId(),
            condominio.tipoCondominio(),
            condominio.sindicoResponsavel(),
            condominio.cadastroMorador(),
            endereco(condominio.endereco()),
            vinculo(condominio.vinculo()),
            condominio.torres().stream()
                .map(torre -> new TorreDto(torre.id(), torre.nome()))
                .toList()
        );
    }

    private VinculoDto vinculo(UsuarioPerfil.Vinculo vinculo) {
        if (vinculo == null) {
            return null;
        }

        return new VinculoDto(
            vinculo.id(),
            vinculo.aprovado(),
            vinculo.dataEntrada(),
            vinculo.dataSaida(),
            vinculo.nivelConfiancaId(),
            vinculo.nivelConfianca(),
            vinculo.trustScore(),
            vinculo.postagensValidadasSemContestacao(),
            vinculo.denunciasRealizadas(),
            vinculo.denunciasProcedentes(),
            vinculo.taxaAcertoDenuncias()
        );
    }
}