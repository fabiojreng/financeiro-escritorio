package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.AgendaPagamentos;
import br.com.escritorio.financeiro.dto.FiltroPagamentos;
import br.com.escritorio.financeiro.dto.PagamentoForm;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Agenda de pagamentos da recepção. Cada pagamento fica ligado direto ao cliente, que é
 * encontrado ou cadastrado pelo CPF no próprio lançamento.
 */
@Service
public class PagamentoService {

    public static final String ENTIDADE = "Pagamento agendado";

    private static final String CAMPO_NOME = "nomeCliente";
    private static final String CAMPO_CPF = "cpf";
    private static final int DIGITOS_MINIMOS_BUSCA_CPF = 3;

    private final PagamentoAgendadoRepository pagamentoRepository;
    private final ResponsavelRepository responsavelRepository;
    private final TipoServicoRepository tipoServicoRepository;
    private final ClienteService clienteService;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public PagamentoService(PagamentoAgendadoRepository pagamentoRepository, ResponsavelRepository responsavelRepository,
                            TipoServicoRepository tipoServicoRepository, ClienteService clienteService,
                            AuditoriaService auditoriaService, Clock relogio) {
        this.pagamentoRepository = pagamentoRepository;
        this.responsavelRepository = responsavelRepository;
        this.tipoServicoRepository = tipoServicoRepository;
        this.clienteService = clienteService;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public PagamentoAgendado buscar(Long id) {
        return pagamentoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Pagamento não encontrado."));
    }

    @Transactional(readOnly = true)
    public AgendaPagamentos agenda(FiltroPagamentos filtro) {
        String termo = filtro.getBusca() == null ? "" : filtro.getBusca().trim();
        String digitos = termo.replaceAll("[\\s.\\-]", "");
        String padraoCpf = digitos.matches("\\d{" + DIGITOS_MINIMOS_BUSCA_CPF + ",11}") ? "%" + digitos + "%" : "";
        List<StatusPagamento> status = filtro.getStatus() == null
                ? Arrays.asList(StatusPagamento.values())
                : List.of(filtro.getStatus());
        return new AgendaPagamentos(pagamentoRepository.pesquisar(filtro.getInicio(), filtro.getFim(), status,
                Textos.padraoBusca(termo), padraoCpf, filtro.getResponsavelId(), filtro.getTipoServicoId()));
    }

    @Transactional
    public PagamentoAgendado criar(PagamentoForm form) {
        validarValor(form.getValor());
        Responsavel responsavel = responsavelAtivo(form.getResponsavelId());
        TipoServico tipo = tipoServicoAtivo(form.getTipoServicoId());
        Cliente cliente = clienteService.obterOuCadastrarPorCpf(form.getNomeCliente(), form.getCpf(), hoje(),
                CAMPO_NOME, CAMPO_CPF);

        PagamentoAgendado pagamento = new PagamentoAgendado(cliente, responsavel, tipo, form.getDataRecebimento(),
                form.getValor(), form.getBanco().trim());
        pagamento.setStatus(form.getStatus());
        pagamento.setObservacoes(Textos.opcional(form.getObservacoes()));
        pagamentoRepository.save(pagamento);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, pagamento.getId(),
                "Pagamento de '" + cliente.getNome() + "' (" + tipo.getNome() + ") agendado para "
                        + Textos.data(pagamento.getDataRecebimento()) + ", valor " + Textos.moeda(pagamento.getValor())
                        + ", banco " + pagamento.getBanco() + ", status " + pagamento.getStatus().getRotulo() + ".");
        return pagamento;
    }

    @Transactional
    public PagamentoAgendado atualizar(Long id, PagamentoForm form) {
        PagamentoAgendado pagamento = buscar(id);
        validarValor(form.getValor());
        List<String> alteracoes = new ArrayList<>();

        Cliente cliente = clienteService.obterOuCadastrarPorCpf(form.getNomeCliente(), form.getCpf(), hoje(),
                CAMPO_NOME, CAMPO_CPF);
        if (!cliente.getId().equals(pagamento.getCliente().getId())) {
            alteracoes.add("cliente de '" + pagamento.getCliente().getNome() + "' para '" + cliente.getNome() + "'");
            pagamento.setCliente(cliente);
        }
        if (!pagamento.getResponsavel().getId().equals(form.getResponsavelId())) {
            Responsavel novo = responsavelAtivo(form.getResponsavelId());
            alteracoes.add("responsável de '" + pagamento.getResponsavel().getNome() + "' para '" + novo.getNome() + "'");
            pagamento.setResponsavel(novo);
        }
        if (!pagamento.getTipoServico().getId().equals(form.getTipoServicoId())) {
            TipoServico novo = tipoServicoAtivo(form.getTipoServicoId());
            alteracoes.add("benefício de '" + pagamento.getTipoServico().getNome() + "' para '" + novo.getNome() + "'");
            pagamento.setTipoServico(novo);
        }
        if (!pagamento.getDataRecebimento().equals(form.getDataRecebimento())) {
            alteracoes.add("dia de recebimento de " + Textos.data(pagamento.getDataRecebimento()) + " para "
                    + Textos.data(form.getDataRecebimento()));
            pagamento.setDataRecebimento(form.getDataRecebimento());
        }
        if (pagamento.getValor().compareTo(form.getValor()) != 0) {
            alteracoes.add("valor de " + Textos.moeda(pagamento.getValor()) + " para " + Textos.moeda(form.getValor()));
            pagamento.setValor(form.getValor());
        }
        String banco = form.getBanco().trim();
        if (!pagamento.getBanco().equals(banco)) {
            alteracoes.add("banco de " + pagamento.getBanco() + " para " + banco);
            pagamento.setBanco(banco);
        }
        if (pagamento.getStatus() != form.getStatus()) {
            alteracoes.add(descricaoStatus(pagamento.getStatus(), form.getStatus()));
            pagamento.setStatus(form.getStatus());
        }
        String observacoes = Textos.opcional(form.getObservacoes());
        if (!Objects.equals(pagamento.getObservacoes(), observacoes)) {
            alteracoes.add("observações");
            pagamento.setObservacoes(observacoes);
        }

        if (!alteracoes.isEmpty()) {
            registrarAlteracao(pagamento, String.join(", ", alteracoes));
        }
        return pagamento;
    }

    @Transactional
    public PagamentoAgendado alterarStatus(Long id, StatusPagamento novo) {
        PagamentoAgendado pagamento = buscar(id);
        StatusPagamento anterior = pagamento.getStatus();
        if (anterior != novo) {
            pagamento.setStatus(novo);
            registrarAlteracao(pagamento, descricaoStatus(anterior, novo));
        }
        return pagamento;
    }

    private static void validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new RegraNegocioException("valor", "O valor deve ser maior que zero.");
        }
    }

    private Responsavel responsavelAtivo(Long id) {
        Responsavel responsavel = responsavelRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("responsavelId", "Responsável não encontrado."));
        if (!responsavel.isAtivo()) {
            throw new RegraNegocioException("responsavelId",
                    "O responsável '" + responsavel.getNome() + "' está inativo e não pode ser usado em novos registros.");
        }
        return responsavel;
    }

    private TipoServico tipoServicoAtivo(Long id) {
        TipoServico tipo = tipoServicoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("tipoServicoId", "Benefício não encontrado."));
        if (!tipo.isAtivo()) {
            throw new RegraNegocioException("tipoServicoId",
                    "O benefício '" + tipo.getNome() + "' está inativo e não pode ser usado em novos registros.");
        }
        return tipo;
    }

    private void registrarAlteracao(PagamentoAgendado pagamento, String alteracoes) {
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, pagamento.getId(),
                "Pagamento de '" + pagamento.getCliente().getNome() + "' alterado: " + alteracoes + ".");
    }

    private static String descricaoStatus(StatusPagamento de, StatusPagamento para) {
        return "status de " + de.getRotulo() + " para " + para.getRotulo();
    }
}
