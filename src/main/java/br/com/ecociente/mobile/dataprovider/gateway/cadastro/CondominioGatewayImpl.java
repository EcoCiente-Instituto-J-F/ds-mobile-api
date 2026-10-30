package br.com.ecociente.mobile.dataprovider.gateway.cadastro;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.cadastro.Condominio;
import br.com.ecociente.mobile.core.domain.cadastro.Morador;
import br.com.ecociente.mobile.core.gateway.cadastro.CondominioGateway;
import br.com.ecociente.mobile.dataprovider.mapper.cadastro.CondominioPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.mapper.cadastro.MoradorPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.CondominioRepository;
import br.com.ecociente.mobile.dataprovider.repository.VinculoUsuarioCondominioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class CondominioGatewayImpl
    implements CondominioGateway {

  private final CondominioRepository condominioRepository;
  private final VinculoUsuarioCondominioRepository vinculoRepository;
  private final CondominioPersistenceMapper condominioMapper;
  private final MoradorPersistenceMapper moradorMapper;
  private final JdbcTemplate jdbc;

  @Override
  @Transactional(readOnly = true)
  public Optional<Condominio> buscarPorCodigoAcesso(
      String codigoAcesso) {
    return condominioRepository
        .findByCodigoAcessoIgnoreCase(codigoAcesso)
        .map(condominioMapper::toDomain);
  }

  @Override
  public Morador vincularMorador(Morador morador) {
    var entity = moradorMapper.toEntity(morador);

    var salvo = vinculoRepository.saveAndFlush(entity);

    jdbc.update(
        """
            INSERT INTO tb_moradores (
                usuario_id,
                condominio_id
            )
            VALUES (?, ?)
            """,
        morador.getUsuarioId(),
        morador.getCondominioId());

    return moradorMapper.toDomain(salvo);
  }
}