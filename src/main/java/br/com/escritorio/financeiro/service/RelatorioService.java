package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.dto.AgendaHonorarios;
import br.com.escritorio.financeiro.dto.AgendaPagamentos;
import br.com.escritorio.financeiro.dto.FiltroRelatorio;
import br.com.escritorio.financeiro.dto.HistoricoCliente;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatórios por período sobre a agenda de pagamentos. Benefício, honorário e repasse nunca se somam.
 */
@Service
public class RelatorioService {

    public static final String ENTIDADE = "Relatório";

    private final PagamentoService pagamentoService;
    private final HonorarioService honorarioService;
    private final ClienteService clienteService;
    private final AuditoriaService auditoriaService;

    public RelatorioService(PagamentoService pagamentoService, HonorarioService honorarioService,
                            ClienteService clienteService, AuditoriaService auditoriaService) {
        this.pagamentoService = pagamentoService;
        this.honorarioService = honorarioService;
        this.clienteService = clienteService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public AgendaPagamentos pagamentos(FiltroRelatorio filtro) {
        return pagamentoService.relatorioPagamentos(filtro);
    }

    @Transactional
    public AgendaHonorarios honorarios(FiltroRelatorio filtro) {
        return honorarioService.relatorio(filtro);
    }

    @Transactional(readOnly = true)
    public AgendaPagamentos repasses(FiltroRelatorio filtro) {
        return pagamentoService.relatorioRepasses(filtro);
    }

    @Transactional
    public HistoricoCliente historico(Long clienteId) {
        return HistoricoCliente.de(clienteService.buscar(clienteId), honorarioService.doCliente(clienteId));
    }

    @Transactional
    public void registrarExportacao(String relatorio, String periodo, int linhas) {
        auditoriaService.registrar(AcaoAuditoria.EXPORTACAO, ENTIDADE, null,
                "Relatório de " + relatorio + " exportado em CSV (" + periodo + ", " + linhas
                        + (linhas == 1 ? " linha)." : " linhas)."));
    }
}
