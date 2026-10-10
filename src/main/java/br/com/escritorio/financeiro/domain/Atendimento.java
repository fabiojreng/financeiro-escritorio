package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Serviço jurídico prestado a um cliente. Previsões de benefício, honorários e repasses
 * pertencem ao atendimento, e não diretamente ao cliente.
 */
@Entity
@Table(name = "atendimento")
public class Atendimento extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_servico_id", nullable = false)
    private TipoServico tipoServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Responsavel responsavel;

    @Column(name = "numero_processo", length = 50)
    private String numeroProcesso;

    @Column(name = "data_abertura", nullable = false)
    private LocalDate dataAbertura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoAtendimento situacao = SituacaoAtendimento.ABERTO;

    @Column(length = 2000)
    private String observacoes;

    protected Atendimento() {
    }

    public Atendimento(Cliente cliente, TipoServico tipoServico, Responsavel responsavel, LocalDate dataAbertura) {
        this.cliente = cliente;
        this.tipoServico = tipoServico;
        this.responsavel = responsavel;
        this.dataAbertura = dataAbertura;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public TipoServico getTipoServico() {
        return tipoServico;
    }

    public void setTipoServico(TipoServico tipoServico) {
        this.tipoServico = tipoServico;
    }

    public Responsavel getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(Responsavel responsavel) {
        this.responsavel = responsavel;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public LocalDate getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDate dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public SituacaoAtendimento getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoAtendimento situacao) {
        this.situacao = situacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
