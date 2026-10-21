package br.com.escritorio.financeiro.domain;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Situação do envio da planilha de despesas de um mês à contabilidade, que deve acontecer até o
 * dia {@value #DIA_LIMITE} do mês seguinte. O aviso começa no dia {@value #DIA_AVISO}.
 */
public enum SituacaoEnvioDespesas {

    EM_ANDAMENTO("Mês em andamento", "text-bg-info",
            "O mês ainda não terminou. Continue lançando as despesas; a planilha é enviada no mês seguinte."),
    AGUARDANDO_ENVIO("Aguardando envio", "text-bg-secondary",
            "O mês terminou e a planilha ainda não foi enviada. O prazo é o dia 20 do mês seguinte."),
    PRAZO_PROXIMO("Prazo próximo", "text-bg-warning",
            "Faltam poucos dias para o dia 20. Confira as despesas, baixe a planilha e envie à contabilidade."),
    ATRASADO("Envio atrasado", "text-bg-danger",
            "O dia 20 já passou e a planilha ainda não foi marcada como enviada."),
    ENVIADO("Enviado à contabilidade", "text-bg-success",
            "A planilha foi enviada. As despesas do mês ficam travadas; quem tem permissão pode reabrir o mês para correções.");

    public static final int DIA_AVISO = 15;
    public static final int DIA_LIMITE = 20;

    private final String rotulo;
    private final String classeBadge;
    private final String descricao;

    SituacaoEnvioDespesas(String rotulo, String classeBadge, String descricao) {
        this.rotulo = rotulo;
        this.classeBadge = classeBadge;
        this.descricao = descricao;
    }

    public static LocalDate prazo(YearMonth competencia) {
        return competencia.plusMonths(1).atDay(DIA_LIMITE);
    }

    public static SituacaoEnvioDespesas de(YearMonth competencia, boolean enviado, LocalDate hoje) {
        if (enviado) {
            return ENVIADO;
        }
        if (!competencia.isBefore(YearMonth.from(hoje))) {
            return EM_ANDAMENTO;
        }
        LocalDate prazo = prazo(competencia);
        if (hoje.isAfter(prazo)) {
            return ATRASADO;
        }
        if (!hoje.isBefore(competencia.plusMonths(1).atDay(DIA_AVISO))) {
            return PRAZO_PROXIMO;
        }
        return AGUARDANDO_ENVIO;
    }

    /** Situações que aparecem como aviso no painel inicial. */
    public boolean isAvisar() {
        return this == PRAZO_PROXIMO || this == ATRASADO;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getClasseBadge() {
        return classeBadge;
    }

    public String getDescricao() {
        return descricao;
    }
}
