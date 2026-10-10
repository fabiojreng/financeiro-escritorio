package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.domain.TipoPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PrevisaoForm {

    private Long id;

    @NotNull(message = "Informe a data prevista.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPrevista;

    @NotNull(message = "Informe o valor estimado.")
    @DecimalMin(value = "0.00", message = "O valor estimado não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valorEstimado;

    @NotNull(message = "Selecione o tipo de pagamento.")
    private TipoPagamento tipoPagamento;

    @NotNull(message = "Selecione a situação.")
    private SituacaoPrevisao situacao = SituacaoPrevisao.PREVISTO;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    public static PrevisaoForm de(PrevisaoBeneficio previsao) {
        PrevisaoForm form = new PrevisaoForm();
        form.id = previsao.getId();
        form.dataPrevista = previsao.getDataPrevista();
        form.valorEstimado = previsao.getValorEstimado();
        form.tipoPagamento = previsao.getTipoPagamento();
        form.situacao = previsao.getSituacao();
        form.observacoes = previsao.getObservacoes();
        return form;
    }

    public boolean isNovo() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public void setDataPrevista(LocalDate dataPrevista) {
        this.dataPrevista = dataPrevista;
    }

    public BigDecimal getValorEstimado() {
        return valorEstimado;
    }

    public void setValorEstimado(BigDecimal valorEstimado) {
        this.valorEstimado = valorEstimado;
    }

    public TipoPagamento getTipoPagamento() {
        return tipoPagamento;
    }

    public void setTipoPagamento(TipoPagamento tipoPagamento) {
        this.tipoPagamento = tipoPagamento;
    }

    public SituacaoPrevisao getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoPrevisao situacao) {
        this.situacao = situacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
