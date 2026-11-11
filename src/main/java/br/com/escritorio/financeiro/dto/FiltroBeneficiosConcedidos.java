package br.com.escritorio.financeiro.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;

/** Mês e filtros da lista de benefícios concedidos. O mês em branco vira o mês atual na tela. */
public class FiltroBeneficiosConcedidos {

    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth mes;

    private String busca;

    private Long tipoServicoId;

    private Long responsavelId;

    private FiltroPagamentoConcedido pagamento = FiltroPagamentoConcedido.TODOS;

    public boolean isFiltrado() {
        return (busca != null && !busca.isBlank()) || tipoServicoId != null || responsavelId != null
                || getPagamento() != FiltroPagamentoConcedido.TODOS;
    }

    public YearMonth getMes() {
        return mes;
    }

    public void setMes(YearMonth mes) {
        this.mes = mes;
    }

    public String getBusca() {
        return busca;
    }

    public void setBusca(String busca) {
        this.busca = busca;
    }

    public Long getTipoServicoId() {
        return tipoServicoId;
    }

    public void setTipoServicoId(Long tipoServicoId) {
        this.tipoServicoId = tipoServicoId;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    public FiltroPagamentoConcedido getPagamento() {
        return pagamento == null ? FiltroPagamentoConcedido.TODOS : pagamento;
    }

    public void setPagamento(FiltroPagamentoConcedido pagamento) {
        this.pagamento = pagamento;
    }
}
