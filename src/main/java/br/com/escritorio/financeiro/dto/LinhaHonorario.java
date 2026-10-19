package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.PagamentoAgendado;

public record LinhaHonorario(PagamentoAgendado pagamento, ValoresHonorario valores) {
}
