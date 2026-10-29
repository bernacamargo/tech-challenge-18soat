package br.com.grupo117.oficina.identidadeacesso.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import br.com.grupo117.oficina.identidadeacesso.domain.Perfil;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guarda de deriva do realm escrito a mao (T14, C27): liga keycloak/realm-oficina.json ao enum
 * Perfil, ao bloco spring.security.oauth2 do application.yml e ao JwtTestSupport, e falha se o
 * realm perder hardening (D12b) ou ganhar segredo versionado. Nenhuma mensagem cita valor de
 * senha ou de campo sensivel: so caminho JSON, chave YAML ou username.
 */
class RealmOficinaConsistenciaTest {

    private static final Path REALM = Path.of("keycloak/realm-oficina.json");
    private static final Path APPLICATION_YML = Path.of("src/main/resources/application.yml");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CLIENTE = "oficina-api";

    // Um nome por linha: o GS do T24 tem excecao explicita so para as 3 linhas de nome de chave/hash (sem valor)
    private static final Set<String> NOMES_PROIBIDOS = Set.of(
            "secret",
            "privateKey",
            "publicKey",
            "certificate",
            "hashedSaltedValue",
            "salt",
            "secretData",
            "credentialData",
            "hashAlgorithm",
            "hashIterations",
            "components",
            "keys",
            "password",
            "id");
    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.eyJ");

    private static JsonNode realm;
    private static Map<String, Object> yaml;

    @BeforeAll
    static void carregar() throws IOException {
        assertTrue(Files.exists(REALM), "realm-oficina.json ausente: rode a partir da raiz do projeto");
        assertTrue(Files.exists(APPLICATION_YML), "application.yml ausente: rode a partir da raiz do projeto");
        realm = MAPPER.readTree(REALM.toFile());
        try (Reader leitor = Files.newBufferedReader(APPLICATION_YML)) {
            yaml = new Yaml().load(leitor);
        }
    }

    // RC1
    @Test
    void perfisDoRealmSaoExatamenteOsDoEnumPerfil() {
        Set<String> nomesDePerfil = Arrays.stream(Perfil.values()).map(Enum::name).collect(Collectors.toSet());

        Set<String> perfisDoRealm = new HashSet<>();
        realm.path("roles").path("realm").forEach(role -> perfisDoRealm.add(role.path("name").asText()));
        assertEquals(nomesDePerfil, perfisDoRealm);

        List<JsonNode> scopeMappings = new ArrayList<>();
        realm.path("scopeMappings").forEach(scopeMappings::add);
        List<JsonNode> doCliente = scopeMappings.stream()
                .filter(sm -> CLIENTE.equals(sm.path("client").asText()))
                .toList();
        assertEquals(1, doCliente.size(), "scopeMappings do cliente " + CLIENTE);
        assertEquals(nomesDePerfil, textos(doCliente.get(0).path("roles")));

        for (JsonNode usuario : realm.path("users")) {
            String username = usuario.path("username").asText();
            Set<String> roles = textos(usuario.path("realmRoles"));
            assertEquals(1, usuario.path("realmRoles").size(), "realmRoles do usuario " + username);
            assertTrue(nomesDePerfil.containsAll(roles), "realmRoles fora do enum Perfil: " + username);
        }
    }

    // RC2
    @Test
    void audienceDoMapperIgualAoAudiencesDoApplicationYml() {
        String audMapper = mapper("oidc-audience-mapper").path("config").path("included.client.audience").asText();
        List<String> audiencesYml = comoLista(valorYaml("spring", "security", "oauth2", "resourceserver", "jwt", "audiences"));

        assertEquals(List.of(audMapper), audiencesYml);
        assertEquals(JwtTestSupport.AUDIENCE, audMapper);
        assertEquals(cliente().path("clientId").asText(), audMapper);
    }

    // RC3
    @Test
    void issuerEAlgoritmoDoApplicationYmlBatemComORealm() {
        String issuerUriCru = (String) valorYaml("spring", "security", "oauth2", "resourceserver", "jwt", "issuer-uri");
        String jwkSetUriCru = (String) valorYaml("spring", "security", "oauth2", "resourceserver", "jwt", "jwk-set-uri");
        List<String> algoritmos = comoLista(valorYaml("spring", "security", "oauth2", "resourceserver", "jwt", "jws-algorithms"));

        String issuer = padraoDoPlaceholder(issuerUriCru);
        String issuerDoRealm = "http://localhost:8081/realms/" + realm.path("realm").asText();
        String jwkEsperado = issuer + "/protocol/openid-connect/certs";
        String algoritmoDoRealm = realm.path("defaultSignatureAlgorithm").asText();

        assertEquals("http://localhost:8081/realms/oficina", issuer);
        assertEquals(issuerDoRealm, issuer);
        assertEquals(JwtTestSupport.ISSUER, issuer);
        assertEquals(jwkEsperado, padraoDoPlaceholder(jwkSetUriCru));
        assertEquals(List.of("RS256"), algoritmos);
        assertEquals("RS256", algoritmoDoRealm);
    }

    // RC4
    @Test
    void clienteOficinaApiPublicoComPasswordGrantSemSegredo() {
        assertEquals(1, realm.path("clients").size(), "quantidade de clients");
        JsonNode cliente = cliente();

        assertCampo(cliente, "enabled", true);
        assertCampo(cliente, "protocol", "openid-connect");
        assertCampo(cliente, "publicClient", true);
        assertCampo(cliente, "bearerOnly", false);
        assertCampo(cliente, "directAccessGrantsEnabled", true);
        assertCampo(cliente, "standardFlowEnabled", false);
        assertCampo(cliente, "implicitFlowEnabled", false);
        assertCampo(cliente, "serviceAccountsEnabled", false);
        assertCampo(cliente, "consentRequired", false);
        assertCampo(cliente, "frontchannelLogout", false);
        assertCampo(cliente, "redirectUris", List.of());
        assertCampo(cliente, "webOrigins", List.of());
        assertCampo(cliente.path("attributes"), "oauth2.device.authorization.grant.enabled", "false");
        assertCampo(cliente.path("attributes"), "oidc.ciba.grant.enabled", "false");
        assertFalse(cliente.has("secret"), "cliente com campo secret");
        assertFalse(cliente.has("clientAuthenticatorType"), "cliente com campo clientAuthenticatorType");
    }

    // RC5
    @Test
    void naoVersionaSegredos() {
        List<String> violacoes = new ArrayList<>();
        varrer(realm, "", violacoes);

        if (!violacoes.isEmpty()) {
            fail("campo proibido no realm: " + String.join(", ", violacoes));
        }
    }

    // RC6
    @Test
    void tokenMantemSubEPreferredUsername() {
        JsonNode cliente = cliente();

        assertCampo(cliente, "defaultClientScopes", List.of("basic"));
        JsonNode opcionais = cliente.path("optionalClientScopes");
        assertTrue(opcionais.isMissingNode() || opcionais.isArray() && opcionais.isEmpty(), "optionalClientScopes nao vazio");
        assertCampo(cliente, "fullScopeAllowed", false);

        List<String> tipos = new ArrayList<>();
        cliente.path("protocolMappers").forEach(m -> tipos.add(m.path("protocolMapper").asText()));
        tipos.sort(null);
        assertEquals(List.of("oidc-audience-mapper", "oidc-usermodel-attribute-mapper", "oidc-usermodel-realm-role-mapper"), tipos);

        JsonNode audience = mapper("oidc-audience-mapper").path("config");
        assertCampo(audience, "included.client.audience", CLIENTE);

        JsonNode username = mapper("oidc-usermodel-attribute-mapper").path("config");
        assertCampo(username, "user.attribute", "username");
        assertCampo(username, "claim.name", "preferred_username");
        assertCampo(username, "userinfo.token.claim", "false");

        JsonNode roles = mapper("oidc-usermodel-realm-role-mapper").path("config");
        assertCampo(roles, "claim.name", "realm_access.roles");
        assertCampo(roles, "multivalued", "true");
        assertCampo(roles, "userinfo.token.claim", "false");

        for (JsonNode config : List.of(audience, username, roles)) {
            assertCampo(config, "access.token.claim", "true");
            assertCampo(config, "id.token.claim", "false");
        }
    }

    // RC7
    @Test
    void hardeningD12b() {
        assertCampo(realm, "enabled", true);
        assertCampo(realm, "sslRequired", "external");
        assertCampo(realm, "defaultSignatureAlgorithm", "RS256");
        assertCampo(realm, "accessTokenLifespan", 900);
        for (String campo : List.of("registrationAllowed", "resetPasswordAllowed", "rememberMe", "verifyEmail",
                "loginWithEmailAllowed", "duplicateEmailsAllowed", "editUsernameAllowed")) {
            assertCampo(realm, campo, false);
        }
        assertCampo(realm, "bruteForceProtected", true);
        assertFalseOuAusente("permanentLockout");
        assertTrue(!realm.has("maxTemporaryLockouts") || realm.get("maxTemporaryLockouts").isInt()
                && realm.get("maxTemporaryLockouts").intValue() == 0, "maxTemporaryLockouts diferente de 0");
        assertCampo(realm, "failureFactor", 5);
        assertCampo(realm, "waitIncrementSeconds", 60);
        assertCampo(realm, "maxFailureWaitSeconds", 900);
        assertCampo(realm, "maxDeltaTimeSeconds", 43200);
        assertCampo(realm, "quickLoginCheckMilliSeconds", 1000);
        assertCampo(realm, "minimumQuickLoginWaitSeconds", 60);

        String politica = realm.path("passwordPolicy").asText();
        assertTrue(Pattern.compile("length\\(8\\)").matcher(politica).find(), "passwordPolicy sem length(8)");
        assertTrue(Pattern.compile("notUsername").matcher(politica).find(), "passwordPolicy sem notUsername");

        assertCampo(realm, "eventsEnabled", true);
        assertCampo(realm, "eventsExpiration", 604800);
        List<String> tipos = new ArrayList<>(textosEmOrdem(realm.path("enabledEventTypes")));
        tipos.sort(null);
        assertEquals(List.of("LOGIN", "LOGIN_ERROR"), tipos);
        assertCampo(realm, "eventsListeners", List.of("jboss-logging"));
        assertFalseOuAusente("adminEventsEnabled");
        assertFalseOuAusente("adminEventsDetailsEnabled");
    }

    // RC8
    @Test
    void usuariosDeDemoCumpremPoliticaDeSenha() {
        Set<String> usernames = new HashSet<>();
        realm.path("users").forEach(u -> usernames.add(u.path("username").asText()));
        assertEquals(Set.of("atendente", "mecanico", "administrador"), usernames);

        for (JsonNode usuario : realm.path("users")) {
            String username = usuario.path("username").asText();
            assertCampo(usuario, "enabled", true);
            assertCampo(usuario, "emailVerified", true);
            assertCampo(usuario, "email", username + "@oficina.local");
            assertFalse(usuario.path("firstName").asText().isBlank(), "firstName vazio: " + username);
            assertFalse(usuario.path("lastName").asText().isBlank(), "lastName vazio: " + username);
            JsonNode acoes = usuario.path("requiredActions");
            assertTrue(acoes.isMissingNode() || acoes.isArray() && acoes.isEmpty(), "requiredActions nao vazio: " + username);
            assertCampo(usuario, "realmRoles", List.of(username.toUpperCase(Locale.ROOT)));

            JsonNode credenciais = usuario.path("credentials");
            assertEquals(1, credenciais.size(), "credenciais do usuario " + username);
            JsonNode credencial = credenciais.get(0);
            assertCampo(credencial, "type", "password");
            assertCampo(credencial, "temporary", false);
            if (!senhaDentroDaPolitica(credencial.path("value"), username)) {
                fail("usuario " + username + ": senha fora da politica");
            }
        }
    }

    // RC9
    @Test
    void smtpApontaParaMailpit() {
        JsonNode smtp = realm.path("smtpServer");

        assertCampo(smtp, "host", "mailpit");
        assertCampo(smtp, "port", "1025");
        assertCampo(smtp, "from", "no-reply@oficina.local");
        assertCampo(smtp, "auth", "false");
        assertCampo(smtp, "ssl", "false");
        assertCampo(smtp, "starttls", "false");
        assertFalse(smtp.has("user"), "smtpServer com campo user");
        assertFalse(smtp.has("password"), "smtpServer com campo password");
    }

    private static JsonNode cliente() {
        List<JsonNode> encontrados = new ArrayList<>();
        realm.path("clients").forEach(c -> {
            if (CLIENTE.equals(c.path("clientId").asText())) {
                encontrados.add(c);
            }
        });
        assertEquals(1, encontrados.size(), "clients com clientId " + CLIENTE);
        return encontrados.get(0);
    }

    private static JsonNode mapper(String protocolMapper) {
        List<JsonNode> encontrados = new ArrayList<>();
        cliente().path("protocolMappers").forEach(m -> {
            if (protocolMapper.equals(m.path("protocolMapper").asText())) {
                encontrados.add(m);
            }
        });
        assertEquals(1, encontrados.size(), "protocolMappers do tipo " + protocolMapper);
        return encontrados.get(0);
    }

    @SuppressWarnings("unchecked")
    private static Object valorYaml(String... chaves) {
        Object atual = yaml;
        for (String chave : chaves) {
            if (!(atual instanceof Map<?, ?> mapa) || !mapa.containsKey(chave)) {
                fail("chave ausente no application.yml: " + String.join(".", chaves));
            }
            atual = ((Map<String, Object>) atual).get(chave);
        }
        return atual;
    }

    private static String padraoDoPlaceholder(String cru) {
        if (cru.startsWith("${") && cru.endsWith("}")) {
            int separador = cru.indexOf(':');
            if (separador < 0) {
                String nome = cru.substring(2, cru.length() - 1);
                fail("placeholder sem padrao: " + nome);
            }
            return cru.substring(separador + 1, cru.length() - 1);
        }
        return cru;
    }

    private static List<String> comoLista(Object valor) {
        if (valor instanceof List<?> lista) {
            return lista.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(valor));
    }

    private static Set<String> textos(JsonNode array) {
        return new HashSet<>(textosEmOrdem(array));
    }

    private static List<String> textosEmOrdem(JsonNode array) {
        List<String> textos = new ArrayList<>();
        array.forEach(n -> textos.add(n.asText()));
        return textos;
    }

    private static void assertCampo(JsonNode no, String campo, Object esperado) {
        assertEquals(MAPPER.valueToTree(esperado), no.path(campo), "campo " + campo);
    }

    private static void assertFalseOuAusente(String campo) {
        JsonNode no = realm.path(campo);
        assertTrue(no.isMissingNode() || no.isBoolean() && !no.booleanValue(), "campo " + campo + " deve ser false ou ausente");
    }

    private static boolean senhaDentroDaPolitica(JsonNode no, String username) {
        if (!no.isTextual()) {
            return false;
        }
        String senha = no.textValue();
        return senha.length() >= 8 && !senha.equalsIgnoreCase(username) && !"demo123".equals(senha);
    }

    private static void varrer(JsonNode no, String caminho, List<String> violacoes) {
        if (no.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> campos = no.fields();
            while (campos.hasNext()) {
                Map.Entry<String, JsonNode> campo = campos.next();
                String filho = caminho.isEmpty() ? campo.getKey() : caminho + "." + campo.getKey();
                if (NOMES_PROIBIDOS.contains(campo.getKey())) {
                    violacoes.add(filho);
                }
                varrer(campo.getValue(), filho, violacoes);
            }
        } else if (no.isArray()) {
            for (int i = 0; i < no.size(); i++) {
                varrer(no.get(i), caminho + "[" + i + "]", violacoes);
            }
        } else if (no.isTextual()) {
            String texto = no.textValue();
            if (texto.contains("-----BEGIN") || JWT.matcher(texto).find() || texto.contains("demo123")) {
                violacoes.add(caminho);
            }
        }
    }
}
