package br.com.grupo117.oficina.identidadeacesso.domain;

/**
 * Portador de credencial valida que nao tem nenhum perfil da oficina (ATENDENTE,
 * MECANICO ou ADMINISTRADOR). Autenticado, mas sem autorizacao: base do 403.
 * A mensagem e fixa e nao contem o sujeito.
 */
public final class UsuarioSemPerfilDaOficinaException extends RuntimeException {

    private final String sujeito;

    public UsuarioSemPerfilDaOficinaException(String sujeito) {
        super("Usuario sem perfil da oficina");
        this.sujeito = sujeito;
    }

    public String sujeito() {
        return sujeito;
    }
}
