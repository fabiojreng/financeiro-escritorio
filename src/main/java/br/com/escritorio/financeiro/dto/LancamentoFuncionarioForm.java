package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import br.com.escritorio.financeiro.domain.TipoLancamentoFuncionario;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class LancamentoFuncionarioForm {

    private Long id;

    @NotNull(message = "Selecione o tipo do lançamento.")
    private TipoLancamentoFuncionario tipo;

    @NotNull(message = "Informe a data.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    /** Vazia: o lançamento entra no mês da data. */
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth competencia;

    @NotNull(message = "Informe o valor.")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valor;

    private FormaPagamento formaPagamento;

    @Size(max = 200, message = "A descrição pode ter no máximo 200 caracteres.")
    private String descricao;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    public static LancamentoFuncionarioForm de(LancamentoFuncionario lancamento) {
        LancamentoFuncionarioForm form = new LancamentoFuncionarioForm();
        form.id = lancamento.getId();
        form.tipo = lancamento.getTipo();
        form.data = lancamento.getData();
        form.competencia = lancamento.getCompetencia();
        form.valor = lancamento.getValor();
        form.formaPagamento = lancamento.getFormaPagamento();
        form.descricao = lancamento.getDescricao();
        form.observacoes = lancamento.getObservacoes();
        return form;
    }

    /** Competência informada ou, se vazia, o mês da data. */
    public YearMonth getCompetenciaEfetiva() {
        if (competencia != null) {
            return competencia;
        }
        return data == null ? null : YearMonth.from(data);
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

    public TipoLancamentoFuncionario getTipo() {
        return tipo;
    }

    public void setTipo(TipoLancamentoFuncionario tipo) {
        this.tipo = tipo;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public YearMonth getCompetencia() {
        return competencia;
    }

    public void setCompetencia(YearMonth competencia) {
        this.competencia = competencia;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
