package br.com.ecociente.mobile.config.validador;

import java.util.List;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.validador")
public record PropriedadesValidador(String chaveApi, String chaveApiAnterior, @NotEmpty List<String> hostsPermitidos, @DecimalMin("0") @DecimalMax("1") double limiarConfianca, @DecimalMin("0") @DecimalMax("1") Double limiarRevisao, String limiaresPorCategoria, @Positive int maxPedidosSimultaneos, @Positive long tamanhoMaximoBytes, @Positive int timeoutDownloadSegundos, @NotBlank String clipDir) {
}
