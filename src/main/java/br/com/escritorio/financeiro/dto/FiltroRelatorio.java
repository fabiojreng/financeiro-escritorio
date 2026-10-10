package br.com.escritorio.financeiro.dto;

import java.time.LocalDate;

/**
 * Filtros comuns dos relatórios. A situação chega como texto porque cada relatório tem a sua
 * lista de situações; um valor desconhecido é tratado como "todas".
 */
public class FiltroRelatorio extends FiltroPeriodo {

    private Long responsavelId;
    private Long tipoServicoId;
    private String situacao;

    public <E extends Enum<E>> E situacaoComo(Class<E> tipo) {
        if (situacao == null || situacao.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, situacao.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Parte do nome do arquivo exportado, como "2026-10" ou "2026-10-01_a_2026-10-31". */
    public String getSufixoArquivo() {
        if (!isPeriodoPersonalizado()) {
            return getMes().toString();
        }
        return texto(getInicioOrdenado(), "inicio") + "_a_" + texto(getFimOrdenado(), "hoje-em-diante");
    }

    private static String texto(LocalDate data, String quandoAberto) {
        return data == null ? quandoAberto : data.toString();
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

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }
}
