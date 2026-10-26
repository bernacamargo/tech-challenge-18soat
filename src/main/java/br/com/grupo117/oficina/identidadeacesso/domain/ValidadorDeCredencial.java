package br.com.grupo117.oficina.identidadeacesso.domain;

/**
 * Contrato de validacao de credencial (port definido no dominio pela issue #4; desvio
 * consciente da ADR 0001, que poe ports na application; sera registrado na ADR de integracao
 * operacional com o Keycloak, issue #6). A implementacao fica na infraestrutura (issue #6).
 */
public interface ValidadorDeCredencial {

    /**
     * Verifica a credencial junto ao provedor de identidade e devolve os dados verificados.
     *
     * @param credencial credencial ja construida (nunca null; o chamador garante)
     * @return dados verificados do portador; nunca null
     * @throws CredencialInvalidaException se o provedor recusar a credencial
     *         (assinatura, expiracao, emissor, audiencia ou formato)
     * @throws RuntimeException outra excecao nao verificada quando o provedor nao puder
     *         responder (indisponivel, erro de rede); isso NAO e credencial invalida e
     *         nao deve ser convertido em CredencialInvalidaException
     */
    DadosDaCredencial validar(Credencial credencial);
}
