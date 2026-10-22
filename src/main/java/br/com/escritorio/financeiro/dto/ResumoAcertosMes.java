package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.AcertoMensal;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Acertos de todos os funcionários numa competência, com os totais do mês. */
public record ResumoAcertosMes(YearMonth competencia, List<AcertoFuncionario> acertos) {

    public AcertoMensal getTotal() {
        return acertos.stream()
                .map(AcertoFuncionario::acerto)
                .reduce(AcertoMensal.ZERADO, AcertoMensal::somar);
    }

    /** Líquido dos acertos ainda não pagos; acertos negativos não entram. */
    public BigDecimal getTotalPendente() {
        return acertos.stream()
                .filter(a -> !a.isPago())
                .map(a -> a.acerto().liquido())
                .filter(valor -> valor.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPago() {
        return acertos.stream()
                .filter(AcertoFuncionario::isPago)
                .map(a -> a.acerto().liquido())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getCusto() {
        return acertos.stream()
                .map(AcertoFuncionario::getCusto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getQuantidadePagos() {
        return acertos.stream().filter(AcertoFuncionario::isPago).count();
    }
}
