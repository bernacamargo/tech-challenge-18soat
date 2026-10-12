package br.com.grupo117.oficina.atendimento.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/**
 * Linha da tabela cliente. Separada do agregado de dominio para o dominio nao depender de JPA.
 */
@Entity
@Table(name = "cliente")
public class ClienteEntity implements Persistable<String> {

    @Id
    @Column(name = "cpf_cnpj", length = 14, nullable = false)
    private String cpfCnpj;

    @Column(name = "nome", length = 150, nullable = false)
    private String nome;

    @Transient
    private boolean novo = true;

    protected ClienteEntity() {
    }

    @Override
    public String getId() {
        return cpfCnpj;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    @PostLoad
    @PostPersist
    void marcarComoExistente() {
        this.novo = false;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
