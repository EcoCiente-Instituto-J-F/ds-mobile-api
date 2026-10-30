package br.com.ecociente.mobile.dataprovider.repository.autenticacao;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ecociente.mobile.dataprovider.entity.autenticacao.AutenticacaoEntity;

public interface AutenticacaoRepository extends JpaRepository<AutenticacaoEntity, Integer> {
    
}
