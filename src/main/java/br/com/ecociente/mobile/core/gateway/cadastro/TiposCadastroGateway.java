package br.com.ecociente.mobile.core.gateway.cadastro;

public interface TiposCadastroGateway {

    Integer usuarioComum();

    Integer cooperativa();

    Integer morador(Integer tipoCondominioId);

    Integer nivelInicial();
}