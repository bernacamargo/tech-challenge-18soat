package br.com.grupo117.oficina.atendimento.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/**
 * Linha da tabela veiculo, com vinculo obrigatorio ao cliente.
 */
@Entity
@Table(name = "veiculo")
public class VeiculoEntity implements Persistable<String> {

    @Id
    @Column(name = "placa", length = 7, nullable = false)
    private String placa;

    @Column(name = "marca", length = 80, nullable = false)
    private String marca;

    @Column(name = "modelo", length = 80, nullable = false)
    private String modelo;

    @Column(name = "ano", nullable = false)
    private int ano;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cpf_cnpj_cliente", nullable = false)
    private ClienteEntity cliente;

    @Transient
    private boolean novo = true;

    protected VeiculoEntity() {
    }

    @Override
    public String getId() {
        return placa;
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

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }

    public ClienteEntity getCliente() {
        return cliente;
    }

    public void setCliente(ClienteEntity cliente) {
        this.cliente = cliente;
    }
}
