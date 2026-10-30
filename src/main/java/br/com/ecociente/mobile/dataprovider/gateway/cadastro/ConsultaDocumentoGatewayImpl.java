package br.com.ecociente.mobile.dataprovider.gateway.cadastro;

import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.exception.cadastro.ConsultaDocumentoIndisponivelException;
import br.com.ecociente.mobile.core.gateway.cadastro.ConsultaDocumentoGateway;
import br.com.ecociente.mobile.dataprovider.client.cadastro.BrasilApiClient;

@Component
public class ConsultaDocumentoGatewayImpl implements ConsultaDocumentoGateway {

    private final BrasilApiClient client;

    public ConsultaDocumentoGatewayImpl(BrasilApiClient client) {
        this.client = client;
    }

    @Override
    public void validarCnpj(String cnpj) {
        Map<String, Object> dados = client.consultarCnpj(cnpj);

        Object documentoRetornado = dados.get("cnpj");
        Object razaoSocial = dados.get("razao_social");

        if (
            !(documentoRetornado instanceof String)
            || !(razaoSocial instanceof String)
        ) {
            throw new ConsultaDocumentoIndisponivelException();
        }

        String cnpjRetornado = normalizar(
            (String) documentoRetornado
        );

        String nomeEmpresa = (String) razaoSocial;

        if (
            !normalizar(cnpj).equals(cnpjRetornado)
            || nomeEmpresa.isBlank()
        ) {
            throw new ConsultaDocumentoIndisponivelException();
        }
    }

    private String normalizar(String documento) {
        return documento
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace(".", "")
            .replace("/", "")
            .replace("-", "");
    }
}