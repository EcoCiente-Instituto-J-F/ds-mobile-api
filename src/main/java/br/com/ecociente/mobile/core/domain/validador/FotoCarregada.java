package br.com.ecociente.mobile.core.domain.validador;

import java.awt.image.BufferedImage;

@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FotoCarregada(BufferedImage imagem, boolean semExif) {
}
