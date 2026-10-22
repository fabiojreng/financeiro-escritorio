package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class EmprestimoFuncionarioForm {

    @NotNull(message = "Informe a data do empréstimo.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataConcessao;

    @NotNull(message = "Informe o valor emprestado.")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valorTotal;

    @NotNull(message = "Informe o número de parcelas.")
    @Min(value = 1, message = "O empréstimo precisa de pelo menos uma parcela.")
    @Max(value = 120, message = "O empréstimo pode ter no máximo 120 parcelas.")
    private Integer numeroParcelas;

    @NotNull(message = "Informe o mês da primeira parcela.")
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth primeiraCompetencia;

    @NotNull(message = "Selecione a forma de pagamento.")
    private FormaPagamento formaPagamento;

    @Size(max = 2000, message = "As observações podem ter no máximo 2000 caracteres.")
    private String observacoes;

    public LocalDate getDataConcessao() {
        return dataConcessao;
    }

    public void setDataConcessao(LocalDate dataConcessao) {
        this.dataConcessao = dataConcessao;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public Integer getNumeroParcelas() {
        return numeroParcelas;
    }

    public void setNumeroParcelas(Integer numeroParcelas) {
        this.numeroParcelas = numeroParcelas;
    }

    public YearMonth getPrimeiraCompetencia() {
        return primeiraCompetencia;
    }

    public void setPrimeiraCompetencia(YearMonth primeiraCompetencia) {
        this.primeiraCompetencia = primeiraCompetencia;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
