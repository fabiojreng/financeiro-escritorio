package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import br.com.escritorio.financeiro.domain.FormaCobranca;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AcordoForm {

    private Long id;

    @NotNull(message = "Selecione a forma de cobrança.")
    private FormaCobranca formaCobranca;

    @DecimalMin(value = "0.00", message = "O valor contratado não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal valorContratado;

    @DecimalMin(value = "0.01", message = "O percentual deve ser maior que zero.")
    @DecimalMax(value = "100.00", message = "O percentual não pode passar de 100.")
    @Digits(integer = 3, fraction = 2, message = "Use no máximo duas casas decimais.")
    private BigDecimal percentual;

    @Size(max = 500, message = "A base de cálculo pode ter no máximo 500 caracteres.")
    private String baseCalculo;

    @Min(value = 1, message = "A quantidade de parcelas deve ser pelo menos 1.")
    @Max(value = 360, message = "A quantidade de parcelas pode ser no máximo 360.")
    private Integer quantidadeParcelas;

    @Size(max = 2000, message = "As condições podem ter no máximo 2000 caracteres.")
    private String condicoes;

    @NotNull(message = "Informe a data do acordo.")
    @PastOrPresent(message = "A data do acordo não pode estar no futuro.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataAcordo = LocalDate.now();

    public static AcordoForm de(AcordoHonorarios acordo) {
        AcordoForm form = new AcordoForm();
        form.id = acordo.getId();
        form.formaCobranca = acordo.getFormaCobranca();
        form.valorContratado = acordo.getValorContratado();
        form.percentual = acordo.getPercentual();
        form.baseCalculo = acordo.getBaseCalculo();
        form.quantidadeParcelas = acordo.getQuantidadeParcelas();
        form.condicoes = acordo.getCondicoes();
        form.dataAcordo = acordo.getDataAcordo();
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

    public FormaCobranca getFormaCobranca() {
        return formaCobranca;
    }

    public void setFormaCobranca(FormaCobranca formaCobranca) {
        this.formaCobranca = formaCobranca;
    }

    public BigDecimal getValorContratado() {
        return valorContratado;
    }

    public void setValorContratado(BigDecimal valorContratado) {
        this.valorContratado = valorContratado;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public void setPercentual(BigDecimal percentual) {
        this.percentual = percentual;
    }

    public String getBaseCalculo() {
        return baseCalculo;
    }

    public void setBaseCalculo(String baseCalculo) {
        this.baseCalculo = baseCalculo;
    }

    public Integer getQuantidadeParcelas() {
        return quantidadeParcelas;
    }

    public void setQuantidadeParcelas(Integer quantidadeParcelas) {
        this.quantidadeParcelas = quantidadeParcelas;
    }

    public String getCondicoes() {
        return condicoes;
    }

    public void setCondicoes(String condicoes) {
        this.condicoes = condicoes;
    }

    public LocalDate getDataAcordo() {
        return dataAcordo;
    }

    public void setDataAcordo(LocalDate dataAcordo) {
        this.dataAcordo = dataAcordo;
    }
}
