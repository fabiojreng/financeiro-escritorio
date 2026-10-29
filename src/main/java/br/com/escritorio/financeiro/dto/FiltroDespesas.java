package br.com.escritorio.financeiro.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;

/** Filtros da lista mensal de despesas. A planilha da contabilidade sempre usa o mês inteiro. */
public class FiltroDespesas {

    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth mes;

    private Long categoriaId;

    private String fornecedor;

    private String observacao;

    public boolean isFiltrado() {
        return categoriaId != null || (fornecedor != null && !fornecedor.isBlank())
                || (observacao != null && !observacao.isBlank());
    }

    public YearMonth getMes() {
        return mes;
    }

    public void setMes(YearMonth mes) {
        this.mes = mes;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(String fornecedor) {
        this.fornecedor = fornecedor;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
