package br.com.escritorio.financeiro.dto;

import java.math.BigDecimal;

/**
 * Indicadores do painel, em dois blocos que nunca se somam: o benefício do INSS pertence ao
 * cliente; honorários e repasses são valores do escritório.
 */
public record PainelIndicadores(ValoresCliente cliente, ValoresEscritorio escritorio) {

    /**
     * @param previsoes            previsões com data prevista no período, exceto as canceladas
     * @param valorEstimado        soma do valor estimado dessas previsões
     * @param valorConfirmado      soma do valor confirmado das liberações com data efetiva no período
     * @param pendentesConfirmacao previsões do período ainda como Previsto ou Aguardando confirmação
     * @param atrasadasNoPeriodo   pendentes do período cuja data prevista já passou
     * @param atrasadasTotal       pendentes com data prevista já passada, de qualquer período
     */
    public record ValoresCliente(long previsoes, BigDecimal valorEstimado, BigDecimal valorConfirmado,
                                 long pendentesConfirmacao, long atrasadasNoPeriodo, long atrasadasTotal) {
    }

    /**
     * @param honorariosContratados    soma do valor contratado dos acordos firmados no período
     * @param acordosSemValor          acordos firmados no período com valor ainda a definir
     * @param honorariosRecebidos      soma dos recebimentos não cancelados com data no período
     * @param honorariosPendentes      saldo em aberto no fim do período: contratado nos acordos firmados
     *                                 até essa data menos o que foi recebido neles até essa data
     * @param repassesPendentes        soma do valor previsto dos repasses pendentes com data prevista até o
     *                                 fim do período, incluindo os atrasados
     * @param quantidadeRepassesPendentes quantidade desses repasses pendentes
     * @param repassesPagos            soma do valor pago dos repasses com data de pagamento no período
     */
    public record ValoresEscritorio(BigDecimal honorariosContratados, long acordosSemValor,
                                    BigDecimal honorariosRecebidos, BigDecimal honorariosPendentes,
                                    BigDecimal repassesPendentes, long quantidadeRepassesPendentes,
                                    BigDecimal repassesPagos) {
    }
}
