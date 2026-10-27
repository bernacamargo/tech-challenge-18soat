package br.com.grupo117.oficina.infra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_JWT = "bearer-jwt";
    static final String URL_TOKEN = "http://localhost:8081/realms/oficina/protocol/openid-connect/token";
    static final String RESPOSTA_401 = "Autenticação necessária";
    static final String RESPOSTA_403 = "Perfil sem permissão";
    static final String DESCRICAO_BEARER = """
            Access token JWT emitido pelo Keycloak (realm `oficina`, client publico `oficina-api`).

            1. Obtenha o token (password grant, habilitado apenas para demonstracao local):

                curl -s -X POST http://localhost:8081/realms/oficina/protocol/openid-connect/token \\
                  -d grant_type=password -d client_id=oficina-api \\
                  -d username=atendente -d password='<senha de demonstracao>' | jq -r .access_token

            2. Usuarios de demonstracao: `atendente` (ATENDENTE), `mecanico` (MECANICO) e
               `administrador` (ADMINISTRADOR). As senhas ficam no README, seção
               "Credenciais de demonstração (somente local)".
            3. Clique em **Authorize**, cole apenas o valor do token (sem o prefixo `Bearer`) e confirme.

            O token vale 15 minutos. Sem token a API responde 401; com perfil sem permissão, 403.
            """;

    @Bean
    OpenAPI oficinaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Oficina")
                        .description("Sistema integrado de atendimento e execucao de servicos")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(DESCRICAO_BEARER)));
    }

    /**
     * Acrescenta 401 e 403 a toda operacao com security, sem sobrescrever o que o controller ja declarou.
     * Operacoes publicas (sem security) ficam como estao.
     */
    @Bean
    OpenApiCustomizer respostasDeSeguranca() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(item -> item.readOperations().forEach(op -> {
                if (op.getSecurity() == null || op.getSecurity().isEmpty()) {
                    return;
                }
                ApiResponses respostas = op.getResponses() != null ? op.getResponses() : new ApiResponses();
                if (!respostas.containsKey("401")) {
                    respostas.addApiResponse("401", new ApiResponse().description(RESPOSTA_401));
                }
                if (!respostas.containsKey("403")) {
                    respostas.addApiResponse("403", new ApiResponse().description(RESPOSTA_403));
                }
                op.setResponses(respostas);
            }));
        };
    }
}
