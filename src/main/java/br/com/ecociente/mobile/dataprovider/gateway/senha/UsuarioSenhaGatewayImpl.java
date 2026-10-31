package br.com.ecociente.mobile.dataprovider.gateway.senha;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.senha.Usuario;
import br.com.ecociente.mobile.core.exception.senha.RecursoNaoEncontradoException;
import br.com.ecociente.mobile.core.gateway.senha.UsuarioGateway;
import br.com.ecociente.mobile.dataprovider.entity.UsuarioEntity;
import br.com.ecociente.mobile.dataprovider.mapper.senha.UsuarioSenhaPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UsuarioSenhaGatewayImpl implements UsuarioGateway {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioSenhaPersistenceMapper usuarioMapper;

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.buscarPorEmailComBloqueio(email.trim())
                .map(usuarioMapper::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorId(Integer id) {
        return usuarioRepository.buscarPorIdComBloqueio(id)
                .map(usuarioMapper::toDomain);
    }

    @Override
    public void atualizarSenha(Integer usuarioId, String senhaHash) {
        UsuarioEntity entity = usuarioRepository.buscarPorIdComBloqueio(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        entity.setSenhaHash(senhaHash);

        usuarioRepository.save(entity);
    }

    @Override
    public Optional<Usuario> buscarAtivoPorId(Integer id) {
        return usuarioRepository.buscarPorIdComBloqueio(id)
                .filter(entity -> Boolean.TRUE.equals(entity.getAtivo()))
                .map(usuarioMapper::toDomain);
    }
}