package br.com.ecociente.mobile.core.domain.validador;

import java.awt.image.BufferedImage;

/**
 * Foto baixada e pronta para o modelo (RGB, em pé).
 * semExif: não traz metadados EXIF (indício fraco: apps de mensagem também removem EXIF).
 */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FotoCarregada(BufferedImage imagem, boolean semExif) {
}
