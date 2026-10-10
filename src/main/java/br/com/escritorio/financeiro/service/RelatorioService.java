package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.CalculoHonorarios;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.domain.SituacaoRepasse;
import br.com.escritorio.financeiro.domain.TotaisRepasses;
import br.com.escritorio.financeiro.dto.AcordoComTotais;
import br.com.escritorio.financeiro.dto.AgendaMensal;
import br.com.escritorio.financeiro.dto.FiltroRecebimento;
import br.com.escritorio.financeiro.dto.FiltroRelatorio;
import br.com.escritorio.financeiro.dto.FiltroSaldo;
import br.com.escritorio.financeiro.dto.HistoricoCliente;
import br.com.escritorio.financeiro.dto.HistoricoCliente.HistoricoAtendimento;
import br.com.escritorio.financeiro.dto.RelatorioAcordos;
import br.com.escritorio.financeiro.dto.RelatorioRecebimentos;
import br.com.escritorio.financeiro.repository.AcordoHonorariosRepository;
import br.com.escritorio.financeiro.repository.PrevisaoBeneficioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import br.com.escritorio.financeiro.repository.RepasseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Relatórios por período. Cada relatório trata de uma única categoria financeira: o benefício do
 * cliente, os honorários ou os repasses nunca são somados entre si.
 */
@Service
public class RelatorioService {

    public static final String ENTIDADE = "Relatório";

    private static final String TODOS_OS_CLIENTES = Textos.padraoBusca(null);

    private final PrevisaoBeneficioRepository previsaoRepository;
    private final AcordoHonorariosRepository acordoRepository;
    private final RecebimentoHonorarioRepository recebimentoRepository;
    private final RepasseRepository repasseRepository;
    private final ClienteService clienteService;
    private final AtendimentoService atendimentoService;
    private final PrevisaoService previsaoService;
    private final HonorariosService honorariosService;
    private final RepasseService repasseService;
    private final AuditoriaService auditoriaService;

    public RelatorioService(PrevisaoBeneficioRepository previsaoRepository, AcordoHonorariosRepository acordoRepository,
                            RecebimentoHonorarioRepository recebimentoRepository, RepasseRepository repasseRepository,
                            ClienteService clienteService, AtendimentoService atendimentoService,
                            PrevisaoService previsaoService, HonorariosService honorariosService,
                            RepasseService repasseService, AuditoriaService auditoriaService) {
        this.previsaoRepository = previsaoRepository;
        this.acordoRepository = acordoRepository;
        this.recebimentoRepository = recebimentoRepository;
        this.repasseRepository = repasseRepository;
        this.clienteService = clienteService;
        this.atendimentoService = atendimentoService;
        this.previsaoService = previsaoService;
        this.honorariosService = honorariosService;
        this.repasseService = repasseService;
        this.auditoriaService = auditoriaService;
    }

    /** Previsões com data prevista no período. */
    @Transactional(readOnly = true)
    public AgendaMensal previsoes(FiltroRelatorio filtro) {
        SituacaoPrevisao situacao = filtro.situacaoComo(SituacaoPrevisao.class);
        List<SituacaoPrevisao> situacoes = situacao == null ? Arrays.asList(SituacaoPrevisao.values()) : List.of(situacao);
        return AgendaMensal.de(previsaoRepository.pesquisar(filtro.getInicioEfetivo(), filtro.getFimEfetivo(),
                situacoes, TODOS_OS_CLIENTES, filtro.getResponsavelId(), filtro.getTipoServicoId()));
    }

    /** Recebimentos de honorários com data de recebimento no período. */
    @Transactional(readOnly = true)
    public RelatorioRecebimentos recebimentos(FiltroRelatorio filtro) {
        FiltroRecebimento situacao = filtro.situacaoComo(FiltroRecebimento.class);
        List<Boolean> cancelados = situacao == null ? List.of(false, true)
                : List.of(situacao == FiltroRecebimento.CANCELADOS);
        return RelatorioRecebimentos.de(recebimentoRepository.relatorio(filtro.getInicioEfetivo(),
                filtro.getFimEfetivo(), cancelados, filtro.getResponsavelId(), filtro.getTipoServicoId()));
    }

    /** Acordos firmados no período, com o recebido e o saldo de hoje. */
    @Transactional(readOnly = true)
    public RelatorioAcordos acordos(FiltroRelatorio filtro) {
        FiltroSaldo saldo = filtro.situacaoComo(FiltroSaldo.class);
        FiltroSaldo filtroSaldo = saldo == null ? FiltroSaldo.TODOS : saldo;
        Map<Long, BigDecimal> recebidos = recebimentoRepository.totaisRecebidosPorAcordo().stream()
                .collect(Collectors.toMap(RecebimentoHonorarioRepository.TotalPorAcordo::getAcordoId,
                        RecebimentoHonorarioRepository.TotalPorAcordo::getTotal));
        List<AcordoComTotais> linhas = acordoRepository.relatorio(filtro.getInicioEfetivo(), filtro.getFimEfetivo(),
                        filtro.getResponsavelId(), filtro.getTipoServicoId()).stream()
                .map(acordo -> {
                    BigDecimal recebido = recebidos.getOrDefault(acordo.getId(), BigDecimal.ZERO);
                    return new AcordoComTotais(acordo, recebido,
                            CalculoHonorarios.saldo(acordo.getValorContratado(), recebido));
                })
                .filter(filtroSaldo::aceita)
                .toList();
        return RelatorioAcordos.de(linhas);
    }

    /** Repasses com data prevista no período; o filtro de responsável vale para o destinatário. */
    @Transactional(readOnly = true)
    public List<Repasse> repasses(FiltroRelatorio filtro) {
        return repasseRepository.relatorio(filtro.getInicioEfetivo(), filtro.getFimEfetivo(),
                filtro.getResponsavelId(), filtro.getTipoServicoId(), filtro.situacaoComo(SituacaoRepasse.class));
    }

    @Transactional(readOnly = true)
    public HistoricoCliente historico(Long clienteId) {
        Cliente cliente = clienteService.buscar(clienteId);
        List<HistoricoAtendimento> atendimentos = atendimentoService.listarPorCliente(clienteId).stream()
                .map(atendimento -> {
                    List<Repasse> repasses = repasseService.listarPorAtendimento(atendimento.getId());
                    return new HistoricoAtendimento(atendimento,
                            AgendaMensal.de(previsaoService.listarPorAtendimento(atendimento.getId())),
                            honorariosService.resumoDoAtendimento(atendimento.getId()),
                            repasses, TotaisRepasses.de(repasses));
                })
                .toList();
        return HistoricoCliente.de(cliente, atendimentos);
    }

    /** Fica no histórico de operações; a descrição não leva dados pessoais. */
    @Transactional
    public void registrarExportacao(String relatorio, String periodo, int linhas) {
        auditoriaService.registrar(AcaoAuditoria.EXPORTACAO, ENTIDADE, null,
                "Relatório de " + relatorio + " exportado em CSV (" + periodo + ", " + linhas
                        + (linhas == 1 ? " linha)." : " linhas)."));
    }
}
