package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.NegociacaoHonorario;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;

import java.util.List;

public record DetalheHonorario(PagamentoAgendado pagamento, ValoresHonorario valores, NegociacaoHonorario negociacao,
                               List<ParcelaComRecebimentos> parcelas, List<NegociacaoHonorario> negociacoesAnteriores,
                               List<Auditoria> historico) {
}
