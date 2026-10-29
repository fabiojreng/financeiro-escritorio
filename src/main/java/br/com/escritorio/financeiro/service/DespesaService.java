package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import br.com.escritorio.financeiro.domain.Despesa;
import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import br.com.escritorio.financeiro.dto.DespesaForm;
import br.com.escritorio.financeiro.repository.CategoriaDespesaRepository;
import br.com.escritorio.financeiro.repository.DespesaRepository;
import br.com.escritorio.financeiro.repository.FormaPagamentoDespesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Gastos avulsos do escritório que vão para a planilha "Contas Pagas" da contabilidade. Uma despesa
 * nunca é apagada: quando lançada por engano, é cancelada com motivo e sai da planilha.
 */
@Service
public class DespesaService {

    public static final String ENTIDADE = "Despesa";

    private final DespesaRepository despesaRepository;
    private final CategoriaDespesaRepository categoriaRepository;
    private final FormaPagamentoDespesaRepository formaPagamentoRepository;
    private final FechamentoDespesaService fechamentoService;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public DespesaService(DespesaRepository despesaRepository, CategoriaDespesaRepository categoriaRepository,
                          FormaPagamentoDespesaRepository formaPagamentoRepository,
                          FechamentoDespesaService fechamentoService, AuditoriaService auditoriaService,
                          Clock relogio) {
        this.despesaRepository = despesaRepository;
        this.categoriaRepository = categoriaRepository;
        this.formaPagamentoRepository = formaPagamentoRepository;
        this.fechamentoService = fechamentoService;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public Despesa buscar(Long id) {
        return despesaRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Despesa não encontrada."));
    }

    /** Despesas do mês, inclusive as canceladas, filtradas por categoria, fornecedor e observações. */
    @Transactional(readOnly = true)
    public List<Despesa> pesquisar(YearMonth mes, Long categoriaId, String fornecedor, String observacao) {
        return despesaRepository.pesquisar(mes.atDay(1), mes.atEndOfMonth(), categoriaId,
                Textos.padraoBusca(fornecedor), Textos.padraoBuscaOpcional(observacao));
    }

    /** Linhas da planilha da contabilidade: só as despesas ativas do mês. */
    @Transactional(readOnly = true)
    public List<Despesa> ativasDoMes(YearMonth mes) {
        return despesaRepository.ativasEntre(mes.atDay(1), mes.atEndOfMonth());
    }

    @Transactional(readOnly = true)
    public List<String> fornecedores() {
        return despesaRepository.fornecedores();
    }

    @Transactional(readOnly = true)
    public List<Auditoria> historico(Long id) {
        return auditoriaService.historico(ENTIDADE, id);
    }

    @Transactional
    public Despesa criar(DespesaForm form) {
        validarData(form.getDataPagamento());
        fechamentoService.exigirAberto(YearMonth.from(form.getDataPagamento()), "dataPagamento");
        CategoriaDespesa categoria = categoriaAtiva(form.getCategoriaId());
        FormaPagamentoDespesa forma = formaPagamentoAtiva(form.getFormaPagamentoId());

        Despesa despesa = new Despesa(form.getDataPagamento(), categoria, forma, form.getHistorico().trim(),
                form.getValor(), form.getFornecedor().trim());
        despesa.setNumeroDocumento(Textos.opcional(form.getNumeroDocumento()));
        despesa.setObservacoes(Textos.opcional(form.getObservacoes()));
        despesaRepository.save(despesa);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, despesa.getId(),
                "Despesa de " + Textos.moeda(despesa.getValor()) + " com '" + despesa.getFornecedor() + "' ("
                        + categoria.getNome() + ", " + forma.getNome() + ") paga em "
                        + Textos.data(despesa.getDataPagamento()) + ".");
        return despesa;
    }

    @Transactional
    public Despesa atualizar(Long id, DespesaForm form) {
        Despesa despesa = buscar(id);
        exigirAtiva(despesa, "alterada");
        fechamentoService.exigirAberto(despesa.getCompetencia(), null);
        validarData(form.getDataPagamento());
        fechamentoService.exigirAberto(YearMonth.from(form.getDataPagamento()), "dataPagamento");

        List<String> alteracoes = new ArrayList<>();
        if (!despesa.getDataPagamento().equals(form.getDataPagamento())) {
            alteracoes.add("data de " + Textos.data(despesa.getDataPagamento()) + " para "
                    + Textos.data(form.getDataPagamento()));
            despesa.setDataPagamento(form.getDataPagamento());
        }
        if (!despesa.getCategoria().getId().equals(form.getCategoriaId())) {
            CategoriaDespesa nova = categoriaAtiva(form.getCategoriaId());
            alteracoes.add("categoria de '" + despesa.getCategoria().getNome() + "' para '" + nova.getNome() + "'");
            despesa.setCategoria(nova);
        }
        if (!despesa.getFormaPagamento().getId().equals(form.getFormaPagamentoId())) {
            FormaPagamentoDespesa nova = formaPagamentoAtiva(form.getFormaPagamentoId());
            alteracoes.add("forma de pagamento de '" + despesa.getFormaPagamento().getNome() + "' para '"
                    + nova.getNome() + "'");
            despesa.setFormaPagamento(nova);
        }
        String historico = form.getHistorico().trim();
        if (!despesa.getHistorico().equals(historico)) {
            alteracoes.add("histórico");
            despesa.setHistorico(historico);
        }
        String numeroDocumento = Textos.opcional(form.getNumeroDocumento());
        if (!Objects.equals(despesa.getNumeroDocumento(), numeroDocumento)) {
            alteracoes.add("número da nota ou referência");
            despesa.setNumeroDocumento(numeroDocumento);
        }
        if (despesa.getValor().compareTo(form.getValor()) != 0) {
            alteracoes.add("valor de " + Textos.moeda(despesa.getValor()) + " para " + Textos.moeda(form.getValor()));
            despesa.setValor(form.getValor());
        }
        String fornecedor = form.getFornecedor().trim();
        if (!despesa.getFornecedor().equals(fornecedor)) {
            alteracoes.add("fornecedor de '" + despesa.getFornecedor() + "' para '" + fornecedor + "'");
            despesa.setFornecedor(fornecedor);
        }
        String observacoes = Textos.opcional(form.getObservacoes());
        if (!Objects.equals(despesa.getObservacoes(), observacoes)) {
            alteracoes.add("observações");
            despesa.setObservacoes(observacoes);
        }
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, despesa.getId(),
                    "Despesa com '" + despesa.getFornecedor() + "' alterada: " + String.join(", ", alteracoes) + ".");
        }
        return despesa;
    }

    /** Cancela com motivo, sem apagar; a despesa deixa de ir para a planilha do mês. */
    @Transactional
    public Despesa cancelar(Long id, String motivo) {
        Despesa despesa = buscar(id);
        String motivoValidado = MotivoCancelamento.validar(motivo);
        exigirAtiva(despesa, "cancelada de novo");
        fechamentoService.exigirAberto(despesa.getCompetencia(), null);
        despesa.cancelar(motivoValidado, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        auditoriaService.registrar(AcaoAuditoria.CANCELAMENTO, ENTIDADE, despesa.getId(),
                "Despesa de " + Textos.moeda(despesa.getValor()) + " com '" + despesa.getFornecedor() + "' ("
                        + Textos.data(despesa.getDataPagamento()) + ") cancelada. Motivo: " + motivoValidado);
        return despesa;
    }

    private void validarData(LocalDate dataPagamento) {
        if (dataPagamento == null || dataPagamento.isAfter(hoje())) {
            throw new RegraNegocioException("dataPagamento",
                    "A data do pagamento não pode estar no futuro. Lance a despesa depois de pagá-la.");
        }
    }

    private static void exigirAtiva(Despesa despesa, String acao) {
        if (despesa.isCancelada()) {
            throw new RegraNegocioException("Esta despesa está cancelada e não pode ser " + acao + ".");
        }
    }

    private CategoriaDespesa categoriaAtiva(Long id) {
        CategoriaDespesa categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("categoriaId", "Categoria não encontrada."));
        if (!categoria.isAtivo()) {
            throw new RegraNegocioException("categoriaId",
                    "A categoria '" + categoria.getNome() + "' está inativa e não pode ser usada em novas despesas.");
        }
        return categoria;
    }

    private FormaPagamentoDespesa formaPagamentoAtiva(Long id) {
        FormaPagamentoDespesa forma = formaPagamentoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("formaPagamentoId", "Forma de pagamento não encontrada."));
        if (!forma.isAtivo()) {
            throw new RegraNegocioException("formaPagamentoId",
                    "A forma de pagamento '" + forma.getNome() + "' está inativa e não pode ser usada em novas despesas.");
        }
        return forma;
    }
}
