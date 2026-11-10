package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.BeneficioConcedido;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.BeneficioConcedidoForm;
import br.com.escritorio.financeiro.dto.FiltroBeneficiosConcedidos;
import br.com.escritorio.financeiro.repository.BeneficioConcedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Benefícios já concedidos, lançados mês a mês, separados da agenda de pagamentos. */
@Service
public class BeneficioConcedidoService {

    public static final String ENTIDADE = "Benefício concedido";

    private static final String ORIGEM = "controle de benefícios concedidos";
    private static final String CAMPO_NOME = "nomeCliente";
    private static final String CAMPO_CPF = "cpf";
    private static final int DIGITOS_MINIMOS_BUSCA_CPF = 3;

    private final BeneficioConcedidoRepository beneficioRepository;
    private final TipoServicoService tipoServicoService;
    private final ResponsavelService responsavelService;
    private final ClienteService clienteService;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public BeneficioConcedidoService(BeneficioConcedidoRepository beneficioRepository,
                                     TipoServicoService tipoServicoService, ResponsavelService responsavelService,
                                     ClienteService clienteService, AuditoriaService auditoriaService, Clock relogio) {
        this.beneficioRepository = beneficioRepository;
        this.tipoServicoService = tipoServicoService;
        this.responsavelService = responsavelService;
        this.clienteService = clienteService;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public BeneficioConcedido buscar(Long id) {
        return beneficioRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Benefício concedido não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<BeneficioConcedido> pesquisar(FiltroBeneficiosConcedidos filtro) {
        String termo = filtro.getBusca() == null ? "" : filtro.getBusca().trim();
        String digitos = termo.replaceAll("[\\s.\\-]", "");
        String padraoCpf = digitos.matches("\\d{" + DIGITOS_MINIMOS_BUSCA_CPF + ",11}") ? "%" + digitos + "%" : "";
        return beneficioRepository.pesquisar(filtro.getMes().atDay(1), Textos.padraoBusca(termo), padraoCpf,
                filtro.getTipoServicoId(), filtro.getResponsavelId());
    }

    @Transactional(readOnly = true)
    public List<Auditoria> historico(Long id) {
        return auditoriaService.historico(ENTIDADE, id);
    }

    @Transactional(readOnly = true)
    public long contar(YearMonth mes) {
        return beneficioRepository.countByCompetencia(mes.atDay(1));
    }

    @Transactional
    public BeneficioConcedido criar(BeneficioConcedidoForm form) {
        Cliente cliente = cliente(form);
        BeneficioConcedido beneficio = new BeneficioConcedido(cliente, tipo(form.getTipoServicoId(), null),
                responsavel(form.getResponsavelId(), null), form.getCompetencia());
        beneficioRepository.save(beneficio);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, beneficio.getId(),
                descricao(beneficio) + " registrado.");
        return beneficio;
    }

    @Transactional
    public BeneficioConcedido atualizar(Long id, BeneficioConcedidoForm form) {
        BeneficioConcedido beneficio = buscar(id);
        List<String> alteracoes = new ArrayList<>();

        Cliente cliente = cliente(form);
        if (!beneficio.getCliente().equals(cliente)) {
            alteracoes.add("cliente de '" + beneficio.getCliente().getNome() + "' para '" + cliente.getNome() + "'");
            beneficio.setCliente(cliente);
        }
        if (!beneficio.getMes().equals(form.getCompetencia())) {
            alteracoes.add("mês de " + Textos.mes(beneficio.getMes()) + " para " + Textos.mes(form.getCompetencia()));
            beneficio.setMes(form.getCompetencia());
        }
        if (!beneficio.getTipoServico().getId().equals(form.getTipoServicoId())) {
            TipoServico tipo = tipo(form.getTipoServicoId(), beneficio.getTipoServico().getId());
            alteracoes.add("benefício de '" + beneficio.getTipoServico().getNome() + "' para '" + tipo.getNome() + "'");
            beneficio.setTipoServico(tipo);
        }
        if (!beneficio.getResponsavel().getId().equals(form.getResponsavelId())) {
            Responsavel responsavel = responsavel(form.getResponsavelId(), beneficio.getResponsavel().getId());
            alteracoes.add("responsável de '" + beneficio.getResponsavel().getNome() + "' para '"
                    + responsavel.getNome() + "'");
            beneficio.setResponsavel(responsavel);
        }
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, beneficio.getId(),
                    descricao(beneficio) + " alterado: " + String.join(", ", alteracoes) + ".");
        }
        return beneficio;
    }

    /** Apaga o lançamento feito por engano ou em duplicidade; os dados e o motivo ficam na auditoria. */
    @Transactional
    public BeneficioConcedido excluir(Long id, String motivo) {
        BeneficioConcedido beneficio = buscar(id);
        String motivoValidado = MotivoCancelamento.validar(motivo, "da exclusão");
        String descricao = descricao(beneficio) + " excluído. Motivo: " + motivoValidado;
        beneficioRepository.delete(beneficio);
        auditoriaService.registrar(AcaoAuditoria.EXCLUSAO, ENTIDADE, id, descricao);
        return beneficio;
    }

    private Cliente cliente(BeneficioConcedidoForm form) {
        return clienteService.obterOuCadastrar(form.getClienteId(), form.getNomeCliente(), form.getCpf(), hoje(),
                ORIGEM, CAMPO_NOME, CAMPO_CPF);
    }

    /** Tipo ativo, ou o tipo que o registro já usa, mesmo que tenha sido desativado. */
    private TipoServico tipo(Long id, Long idAtual) {
        TipoServico tipo = tipoServicoService.buscar(id);
        if (!tipo.isAtivo() && !Objects.equals(tipo.getId(), idAtual)) {
            throw new RegraNegocioException("tipoServicoId",
                    "O benefício '" + tipo.getNome() + "' está inativo e não pode ser usado em novos registros.");
        }
        return tipo;
    }

    /** Responsável ativo, ou o responsável que o registro já usa, mesmo que tenha sido desativado. */
    private Responsavel responsavel(Long id, Long idAtual) {
        Responsavel responsavel = responsavelService.buscar(id);
        if (!responsavel.isAtivo() && !Objects.equals(responsavel.getId(), idAtual)) {
            throw new RegraNegocioException("responsavelId",
                    "O responsável '" + responsavel.getNome() + "' está inativo e não pode ser usado em novos registros.");
        }
        return responsavel;
    }

    private static String descricao(BeneficioConcedido beneficio) {
        return "Benefício '" + beneficio.getTipoServico().getNome() + "' de '" + beneficio.getCliente().getNome()
                + "', responsável '" + beneficio.getResponsavel().getNome() + "', em " + Textos.mes(beneficio.getMes());
    }
}
