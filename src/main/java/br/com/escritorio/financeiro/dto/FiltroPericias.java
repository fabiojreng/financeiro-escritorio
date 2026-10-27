package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.SituacaoPericia;

/** Filtros da agenda de perícias: o período (ver {@link FiltroAgenda}) e os filtros próprios das perícias. */
public class FiltroPericias extends FiltroAgenda {

    private String busca;
    private Long responsavelId;
    private Long tipoPericiaId;
    private Long localId;
    private SituacaoPericia situacao;

    public String getBusca() {
        return busca;
    }

    public void setBusca(String busca) {
        this.busca = busca;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    public Long getTipoPericiaId() {
        return tipoPericiaId;
    }

    public void setTipoPericiaId(Long tipoPericiaId) {
        this.tipoPericiaId = tipoPericiaId;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    public SituacaoPericia getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoPericia situacao) {
        this.situacao = situacao;
    }
}
