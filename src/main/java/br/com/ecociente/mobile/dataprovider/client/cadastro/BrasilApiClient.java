package br.com.ecociente.mobile.dataprovider.client.cadastro;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import br.com.ecociente.mobile.core.exception.cadastro.BusinessException;
import br.com.ecociente.mobile.core.exception.cadastro.ConsultaDocumentoIndisponivelException;

@Component
public class BrasilApiClient {

    private static final String URL =
        "https://brasilapi.com.br/api/cnpj/v1/{cnpj}";

    private static final ParameterizedTypeReference<Map<String, Object>> JSON =
        new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;

    public BrasilApiClient() {
        var factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.restTemplate = new RestTemplate(factory);
    }

    public Map<String, Object> consultarCnpj(String cnpj) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            var resposta = restTemplate.exchange(
                URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JSON,
                cnpj
            );

            if (
                resposta.getStatusCode().value() != 200
                || resposta.getBody() == null
            ) {
                throw new ConsultaDocumentoIndisponivelException();
            }

            return resposta.getBody();

        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();

            if (status == 404) {
                throw new BusinessException(
                    "CNPJ_NOT_FOUND",
                    "CNPJ não encontrado na base consultada"
                );
            }

            if (status == 400) {
                throw new BusinessException(
                    "CNPJ_INVALID",
                    "CNPJ rejeitado pela consulta. Verifique o documento informado."
                );
            }

            // Falhas do fornecedor e limites de acesso não são CNPJ inválido.
            throw new ConsultaDocumentoIndisponivelException();

        } catch (RestClientException ex) {
            throw new ConsultaDocumentoIndisponivelException();
        }
    }
}