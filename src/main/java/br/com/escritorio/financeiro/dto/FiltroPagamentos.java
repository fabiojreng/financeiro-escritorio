package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;

/** Filtros da agenda de pagamentos: o período (ver {@link FiltroAgenda}) e os filtros próprios dos pagamentos. */
public class FiltroPagamentos extends FiltroAgenda {

    private String busca;
    private String observacao;
    private Long responsavelId;
    private Long tipoServicoId;
    private StatusPagamento status;
    private SituacaoHonorarioPagamento situacaoHonorario;
    private SituacaoRepassePagamento situacaoRepasse;

    public String getBusca() {
        return busca;
    }

    public void setBusca(String busca) {
        this.busca = busca;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    public Long getTipoServicoId() {
        return tipoServicoId;
    }

    public void setTipoServicoId(Long tipoServicoId) {
        this.tipoServicoId = tipoServicoId;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public void setStatus(StatusPagamento status) {
        this.status = status;
    }

    public SituacaoHonorarioPagamento getSituacaoHonorario() {
        return situacaoHonorario;
    }

    public void setSituacaoHonorario(SituacaoHonorarioPagamento situacaoHonorario) {
        this.situacaoHonorario = situacaoHonorario;
    }

    public SituacaoRepassePagamento getSituacaoRepasse() {
        return situacaoRepasse;
    }

    public void setSituacaoRepasse(SituacaoRepassePagamento situacaoRepasse) {
        this.situacaoRepasse = situacaoRepasse;
    }
}
