package br.com.grupo117.oficina.infra.security;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor HTTP do JDK que publica o JWKS de teste (so a chave publica de JwtTestSupport),
 * escutando apenas em loopback, em porta efemera.
 */
public final class JwksTestServer {

    public static final String CAMINHO = "/realms/oficina/protocol/openid-connect/certs";

    // Instancia unica pela JVM inteira; nao e parada porque o contexto Spring em cache
    // continuaria apontando para uma porta que ja nao escuta.
    private static final HttpServer SERVIDOR;
    private static final AtomicInteger REQUISICOES = new AtomicInteger();

    static {
        try {
            SERVIDOR = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao iniciar o servidor JWKS de teste", e);
        }
        SERVIDOR.createContext(CAMINHO, troca -> {
            try (troca) {
                if (!"GET".equals(troca.getRequestMethod())) {
                    troca.sendResponseHeaders(405, -1);
                    return;
                }
                REQUISICOES.incrementAndGet();
                byte[] corpo = JwtTestSupport.jwksJson().getBytes(StandardCharsets.UTF_8);
                troca.getResponseHeaders().set("Content-Type", "application/json");
                troca.sendResponseHeaders(200, corpo.length);
                try (OutputStream saida = troca.getResponseBody()) {
                    saida.write(corpo);
                }
            }
        });
        SERVIDOR.start();
    }

    private JwksTestServer() {
    }

    public static String uri() {
        InetSocketAddress endereco = SERVIDOR.getAddress();
        try {
            return new URI("http", null, endereco.getHostString(), endereco.getPort(), CAMINHO, null, null)
                    .toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Falha ao montar URI do JWKS de teste", e);
        }
    }

    static InetSocketAddress enderecoDeEscuta() {
        return SERVIDOR.getAddress();
    }

    public static int requisicoes() {
        return REQUISICOES.get();
    }
}
