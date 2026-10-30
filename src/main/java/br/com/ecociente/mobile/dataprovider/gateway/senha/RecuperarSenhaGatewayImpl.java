package br.com.ecociente.mobile.dataprovider.gateway.senha;

import br.com.ecociente.mobile.core.domain.senha.RecuperacaoSenha;
import br.com.ecociente.mobile.core.gateway.senha.RecuperacaoSenhaGateway;
import br.com.ecociente.mobile.dataprovider.entity.EsqueceuSenhaEntity;
import br.com.ecociente.mobile.dataprovider.mapper.senha.RecuperarSenhaMapper;
import br.com.ecociente.mobile.dataprovider.repository.EsqueceuSenhaRepositoy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Optional;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class RecuperarSenhaGatewayImpl
                implements RecuperacaoSenhaGateway {

        private final EsqueceuSenhaRepositoy esqueciSenhaRepository;
        private final RecuperarSenhaMapper recuperarSenhaMapper;

        @Override
        public RecuperacaoSenha salvar(RecuperacaoSenha recuperacaoSenha) {
                EsqueceuSenhaEntity entity = recuperarSenhaMapper.toEntity(recuperacaoSenha);

                EsqueceuSenhaEntity entitySalva = esqueciSenhaRepository.save(entity);

                return recuperarSenhaMapper.toDomain(entitySalva);
        }

        @Override
        public Optional<RecuperacaoSenha> buscarUltimaPorUsuarioId(
                        Integer usuarioId) {
                return esqueciSenhaRepository
                                .findFirstByUsuarioIdOrderByIdDesc(usuarioId)
                                .map(recuperarSenhaMapper::toDomain);
        }

        @Override
        public void invalidarPendentes(
                        Integer usuarioId,
                        Timestamp agora) {

                esqueciSenhaRepository.invalidarPendentes(
                                usuarioId,
                                agora.toInstant().atOffset(ZoneOffset.UTC));
        }
}