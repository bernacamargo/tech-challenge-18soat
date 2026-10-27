package br.com.grupo117.oficina.infra.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.function.Consumer;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

public final class JwtTestSupport {

    // Unico HMAC do repositorio: so para tokenHs256 provar que o decoder recusa HS256.
    private static final String SEGREDO_HS256 = "chave-de-teste-da-oficina-grupo117";

    // Constantes literais: nunca lidas de propriedades (um erro no application.yml tem de quebrar a suite).
    public static final String ISSUER = "http://localhost:8081/realms/oficina";
    public static final String AUDIENCE = "oficina-api";
    public static final String KID = "teste-oficina";
    public static final String SUJEITO = "usuario-teste";
    public static final String LOGIN = "usuario-teste";
    public static final List<String> ROLES_TECNICOS =
            List.of("default-roles-oficina", "offline_access", "uma_authorization");

    // Ordem obrigatoria: constantes, depois chaves, depois encoders (inicializacao estatica segue o fonte).
    static final RSAKey CHAVE = gerarChave(KID);
    static final RSAKey CHAVE_MESMO_KID = gerarChave(KID);
    static final RSAKey CHAVE_KID_DESCONHECIDO = gerarChave("kid-desconhecido");

    private static final NimbusJwtEncoder ENCODER = encoderDe(CHAVE);
    private static final NimbusJwtEncoder ENCODER_MESMO_KID = encoderDe(CHAVE_MESMO_KID);
    private static final NimbusJwtEncoder ENCODER_KID_DESCONHECIDO = encoderDe(CHAVE_KID_DESCONHECIDO);

    private JwtTestSupport() {
    }

    public static String token(String... perfis) {
        return tokenRs256(perfis);
    }

    public static String jwksJson() {
        return new JWKSet(CHAVE.toPublicJWK()).toString();
    }

    public static RSAPublicKey chavePublica() {
        try {
            return CHAVE.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException("Falha ao ler chave publica de teste", e);
        }
    }

    public static String tokenRs256(String... perfis) {
        return tokenRs256(b -> { }, perfis);
    }

    public static String tokenRs256(Consumer<JwtClaimsSet.Builder> ajuste, String... perfis) {
        return assinar(ENCODER, KID, ajuste, perfis);
    }

    public static String tokenExpirado() {
        Instant agora = Instant.now();
        return tokenRs256(b -> b.issuedAt(agora.minusSeconds(420)).expiresAt(agora.minusSeconds(120)), "ATENDENTE");
    }

    public static String tokenNaoAntesDe() {
        return tokenRs256(b -> b.notBefore(Instant.now().plusSeconds(120)), "ATENDENTE");
    }

    public static String tokenComIssuer(String issuer) {
        return tokenRs256(b -> b.issuer(issuer), "ATENDENTE");
    }

    @SuppressWarnings("unchecked")
    public static String tokenComAudience(Object audience) {
        List<String> lista = audience instanceof List<?> valores ? (List<String>) valores : List.of((String) audience);
        return tokenRs256(b -> b.audience(lista), "ATENDENTE");
    }

    public static String tokenSemAudience() {
        return tokenRs256(b -> b.claims(c -> c.remove("aud")), "ATENDENTE");
    }

    public static String tokenSemExp() {
        return tokenRs256(b -> b.claims(c -> c.remove("exp")), "ATENDENTE");
    }

    public static String tokenDeId() {
        return tokenRs256(b -> b.claim("typ", "ID"), "ATENDENTE");
    }

    public static String tokenSemSub() {
        return tokenRs256(b -> b.claims(c -> c.remove("sub")), "ATENDENTE");
    }

    public static String tokenSemPreferredUsername() {
        return tokenRs256(b -> b.claims(c -> c.remove("preferred_username")), "ATENDENTE");
    }

    public static String tokenSemPerfilDaOficina() {
        return tokenRs256(ROLES_TECNICOS.toArray(String[]::new));
    }

    public static String tokenAssinadoComOutraChave() {
        return assinar(ENCODER_MESMO_KID, KID, b -> { }, "ATENDENTE");
    }

    public static String tokenComKidDesconhecido() {
        return assinar(ENCODER_KID_DESCONHECIDO, "kid-desconhecido", b -> { }, "ATENDENTE");
    }

    public static String tokenAlgNone() {
        Instant agora = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .subject(SUJEITO)
                .claim("preferred_username", LOGIN)
                .claim("typ", "Bearer")
                .claim("azp", AUDIENCE)
                .issueTime(Date.from(agora))
                .expirationTime(Date.from(agora.plusSeconds(300)))
                .claim("realm_access", Map.of("roles", List.of("ATENDENTE")))
                .build();
        return new PlainJWT(claims).serialize();
    }

    public static String tokenHs256(String... perfis) {
        SecretKey chave = new SecretKeySpec(SEGREDO_HS256.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claimsPadrao(Instant.now(), perfis).build()))
                .getTokenValue();
    }

    private static String assinar(NimbusJwtEncoder encoder, String kid, Consumer<JwtClaimsSet.Builder> ajuste,
            String... perfis) {
        JwtClaimsSet.Builder claims = claimsPadrao(Instant.now(), perfis);
        ajuste.accept(claims);
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(kid).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private static JwtClaimsSet.Builder claimsPadrao(Instant agora, String... perfis) {
        return JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .subject(SUJEITO)
                .claim("preferred_username", LOGIN)
                .claim("typ", "Bearer")
                .claim("azp", AUDIENCE)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(300))
                .claim("realm_access", Map.of("roles", List.of(perfis)));
    }

    private static RSAKey gerarChave(String kid) {
        try {
            return new RSAKeyGenerator(2048)
                    .keyID(kid)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(KeyUse.SIGNATURE)
                    .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("Falha ao gerar chave RSA de teste", e);
        }
    }

    private static NimbusJwtEncoder encoderDe(RSAKey chave) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(chave)));
    }
}
