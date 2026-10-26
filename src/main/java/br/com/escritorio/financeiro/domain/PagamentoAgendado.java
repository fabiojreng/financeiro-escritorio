package br.com.escritorio.financeiro.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pagamento liberado que o cliente vai receber no banco, lançado pela recepção na agenda semanal.
 * Fica ligado direto ao cliente, sem o ciclo antigo de atendimento, e pode ter vários responsáveis.
 */
@Entity
@Table(name = "pagamento_agendado")
public class PagamentoAgendado extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "pagamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ResponsavelPagamento> responsaveis = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_servico_id", nullable = false)
    private TipoServico tipoServico;

    @Column(name = "data_recebimento", nullable = false)
    private LocalDate dataRecebimento;

    /** Valor do benefício; em branco enquanto ainda não se sabe quanto o cliente vai receber. */
    @Column(precision = 15, scale = 2)
    private BigDecimal valor;

    /** Banco em que o cliente vai receber; em branco enquanto ainda não se sabe. */
    @Column(length = 80)
    private String banco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status = StatusPagamento.AGENDADO;

    @Column(length = 2000)
    private String observacoes;

    @Column(name = "valor_honorario", precision = 15, scale = 2)
    private BigDecimal valorHonorario;

    /** Parte do honorário já paga quando a situação é "Parcial" sem negociação. */
    @Column(name = "valor_honorario_pago", precision = 15, scale = 2)
    private BigDecimal valorHonorarioPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_honorario", nullable = false, length = 20)
    private SituacaoHonorarioPagamento situacaoHonorario = SituacaoHonorarioPagamento.A_RECEBER;

    protected PagamentoAgendado() {
    }

    public PagamentoAgendado(Cliente cliente, List<Responsavel> responsaveis, TipoServico tipoServico,
                             LocalDate dataRecebimento, BigDecimal valor, String banco) {
        this.cliente = cliente;
        responsaveis.forEach(this::adicionarResponsavel);
        this.tipoServico = tipoServico;
        this.dataRecebimento = dataRecebimento;
        this.valor = valor;
        this.banco = banco;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    /** Responsáveis do pagamento em ordem alfabética, cada um com o próprio repasse. */
    public List<ResponsavelPagamento> getResponsaveis() {
        return responsaveis.stream()
                .sorted(Comparator.comparing((ResponsavelPagamento r) -> r.getResponsavel().getNome(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public ResponsavelPagamento adicionarResponsavel(Responsavel responsavel) {
        ResponsavelPagamento vinculo = new ResponsavelPagamento(this, responsavel);
        responsaveis.add(vinculo);
        return vinculo;
    }

    public void removerResponsavel(ResponsavelPagamento vinculo) {
        responsaveis.remove(vinculo);
    }

    public Optional<ResponsavelPagamento> responsavel(Long responsavelId) {
        return responsaveis.stream()
                .filter(r -> r.getResponsavel().getId().equals(responsavelId))
                .findFirst();
    }

    /** Nomes dos responsáveis separados por vírgula, por exemplo "Ana, Bruno". */
    public String getNomesResponsaveis() {
        return getResponsaveis().stream()
                .map(r -> r.getResponsavel().getNome())
                .collect(Collectors.joining(", "));
    }

    /** Soma dos repasses lançados aos responsáveis; {@code null} quando nenhum repasse foi lançado. */
    public BigDecimal getTotalRepasse() {
        return responsaveis.stream()
                .map(ResponsavelPagamento::getValorRepasse)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(null);
    }

    public TipoServico getTipoServico() {
        return tipoServico;
    }

    public void setTipoServico(TipoServico tipoServico) {
        this.tipoServico = tipoServico;
    }

    public LocalDate getDataRecebimento() {
        return dataRecebimento;
    }

    public void setDataRecebimento(LocalDate dataRecebimento) {
        this.dataRecebimento = dataRecebimento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public void setStatus(StatusPagamento status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public BigDecimal getValorHonorario() {
        return valorHonorario;
    }

    public void setValorHonorario(BigDecimal valorHonorario) {
        this.valorHonorario = valorHonorario;
    }

    public BigDecimal getValorHonorarioPago() {
        return valorHonorarioPago;
    }

    public void setValorHonorarioPago(BigDecimal valorHonorarioPago) {
        this.valorHonorarioPago = valorHonorarioPago;
    }

    public SituacaoHonorarioPagamento getSituacaoHonorario() {
        return situacaoHonorario;
    }

    public void setSituacaoHonorario(SituacaoHonorarioPagamento situacaoHonorario) {
        this.situacaoHonorario = situacaoHonorario;
    }
}
