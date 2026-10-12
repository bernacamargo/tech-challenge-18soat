package br.com.grupo117.oficina.infra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_JWT = "bearer-jwt";

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
                        .description("JWT emitido pelo Keycloak, realm oficina")));
    }
}
