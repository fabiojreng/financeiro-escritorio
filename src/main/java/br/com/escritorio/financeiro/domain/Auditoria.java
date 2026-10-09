package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false, updatable = false, length = 50)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private AcaoAuditoria acao;

    @Column(nullable = false, updatable = false, length = 50)
    private String entidade;

    @Column(name = "entidade_id", updatable = false)
    private Long entidadeId;

    @Column(nullable = false, updatable = false, length = 500)
    private String descricao;

    protected Auditoria() {
    }

    public Auditoria(String usuario, AcaoAuditoria acao, String entidade, Long entidadeId, String descricao) {
        this.dataHora = LocalDateTime.now();
        this.usuario = usuario;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public AcaoAuditoria getAcao() {
        return acao;
    }

    public String getEntidade() {
        return entidade;
    }

    public Long getEntidadeId() {
        return entidadeId;
    }

    public String getDescricao() {
        return descricao;
    }
}
