package br.com.ecociente.mobile.core.domain.validador;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class Categorias {

    public static final List<Classe> CATEGORIAS = List.of(new Classe("papel", "papel", List.of("a photo of cardboard boxes", "a photo of paper waste", "a photo of old newspapers and magazines")), new Classe("plastico", "plástico", List.of("a photo of a plastic bottle", "a photo of plastic packaging", "a photo of plastic bags")), new Classe("vidro", "vidro", List.of("a photo of a glass bottle", "a photo of a glass jar", "a photo of broken glass")), new Classe("metal", "metal", List.of("a photo of an aluminum can", "a photo of a tin can", "a photo of scrap metal")), new Classe("organico", "orgânico", List.of("a photo of food scraps", "a photo of fruit and vegetable peels", "a photo of coffee grounds and eggshells")));

    public static final Classe NAO_RESIDUO = new Classe("nao_residuo", "não resíduo", List.of("a photo of a person", "a selfie", "a photo of a pet", "a screenshot of a phone screen", "a blurry dark photo"));

    public static final List<Classe> TODAS_AS_CLASSES;

    private static final Map<String, Classe> POR_SLUG = CATEGORIAS.stream().collect(Collectors.toUnmodifiableMap(Classe::slug, Function.identity()));

    static {
        List<Classe> todas = new ArrayList<>(CATEGORIAS);
        todas.add(NAO_RESIDUO);
        TODAS_AS_CLASSES = List.copyOf(todas);
    }

    private Categorias() {
    }

    public static Optional<Classe> porSlug(String slug) {
        return Optional.ofNullable(POR_SLUG.get(slug));
    }

    public static Classe categoria(String slug) {
        return porSlug(slug).orElseThrow(() -> new IllegalArgumentException("categoria fora do catálogo: " + slug));
    }

    public static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFKD).replaceAll("[^\\x00-\\x7F]", "");
        return String.join("_", semAcento.toLowerCase(Locale.ROOT).trim().split("\\s+"));
    }
}
