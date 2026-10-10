package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.AtendimentoForm;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AtendimentoService {

    public static final String ENTIDADE = "Atendimento";

    static final int TAMANHO_PAGINA = 20;

    private final AtendimentoRepository atendimentoRepository;
    private final ClienteRepository clienteRepository;
    private final TipoServicoRepository tipoServicoRepository;
    private final ResponsavelRepository responsavelRepository;
    private final AuditoriaService auditoriaService;

    public AtendimentoService(AtendimentoRepository atendimentoRepository, ClienteRepository clienteRepository,
                              TipoServicoRepository tipoServicoRepository, ResponsavelRepository responsavelRepository,
                              AuditoriaService auditoriaService) {
        this.atendimentoRepository = atendimentoRepository;
        this.clienteRepository = clienteRepository;
        this.tipoServicoRepository = tipoServicoRepository;
        this.responsavelRepository = responsavelRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public Page<Atendimento> pesquisar(String buscaCliente, Long responsavelId, Long tipoServicoId,
                                       SituacaoAtendimento situacao, int pagina) {
        return atendimentoRepository.pesquisar(Textos.padraoBusca(buscaCliente), responsavelId, tipoServicoId,
                situacao, PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA));
    }

    @Transactional(readOnly = true)
    public List<Atendimento> listarPorCliente(Long clienteId) {
        return atendimentoRepository.findByClienteIdOrderByDataAberturaDescIdDesc(clienteId);
    }

    @Transactional(readOnly = true)
    public Atendimento buscar(Long id) {
        return atendimentoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Atendimento não encontrado."));
    }

    @Transactional
    public Atendimento criar(AtendimentoForm form) {
        Cliente cliente = clienteRepository.findById(form.getClienteId())
                .orElseThrow(() -> new RegraNegocioException("clienteId", "Cliente não encontrado."));
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("clienteId",
                    "O cliente está inativo. Reative o cadastro antes de abrir um novo atendimento.");
        }
        TipoServico tipo = tipoServicoAtivo(form.getTipoServicoId());
        Responsavel responsavel = responsavelAtivo(form.getResponsavelId());

        Atendimento atendimento = new Atendimento(cliente, tipo, responsavel, form.getDataAbertura());
        atendimento.setNumeroProcesso(Textos.opcional(form.getNumeroProcesso()));
        atendimento.setSituacao(form.getSituacao());
        atendimento.setObservacoes(Textos.opcional(form.getObservacoes()));
        atendimentoRepository.save(atendimento);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, atendimento.getId(),
                "Atendimento de '" + cliente.getNome() + "' (" + tipo.getNome() + ") aberto com responsável '"
                        + responsavel.getNome() + "'.");
        return atendimento;
    }

    @Transactional
    public Atendimento atualizar(Long id, AtendimentoForm form) {
        Atendimento atendimento = buscar(id);
        List<String> alteracoes = new ArrayList<>();

        if (!atendimento.getTipoServico().getId().equals(form.getTipoServicoId())) {
            TipoServico novo = tipoServicoAtivo(form.getTipoServicoId());
            alteracoes.add("tipo de serviço de '" + atendimento.getTipoServico().getNome() + "' para '"
                    + novo.getNome() + "'");
            atendimento.setTipoServico(novo);
        }
        if (!atendimento.getResponsavel().getId().equals(form.getResponsavelId())) {
            Responsavel novo = responsavelAtivo(form.getResponsavelId());
            alteracoes.add("responsável de '" + atendimento.getResponsavel().getNome() + "' para '"
                    + novo.getNome() + "'");
            atendimento.setResponsavel(novo);
        }
        String processo = Textos.opcional(form.getNumeroProcesso());
        if (!Objects.equals(atendimento.getNumeroProcesso(), processo)) {
            alteracoes.add("número do processo");
            atendimento.setNumeroProcesso(processo);
        }
        if (!atendimento.getDataAbertura().equals(form.getDataAbertura())) {
            alteracoes.add("data de abertura");
            atendimento.setDataAbertura(form.getDataAbertura());
        }
        if (atendimento.getSituacao() != form.getSituacao()) {
            alteracoes.add("situação de " + atendimento.getSituacao().getRotulo() + " para "
                    + form.getSituacao().getRotulo());
            atendimento.setSituacao(form.getSituacao());
        }
        String observacoes = Textos.opcional(form.getObservacoes());
        if (!Objects.equals(atendimento.getObservacoes(), observacoes)) {
            alteracoes.add("observações");
            atendimento.setObservacoes(observacoes);
        }

        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, atendimento.getId(),
                    "Atendimento de '" + atendimento.getCliente().getNome() + "' alterado: "
                            + String.join(", ", alteracoes) + ".");
        }
        return atendimento;
    }

    private TipoServico tipoServicoAtivo(Long id) {
        TipoServico tipo = tipoServicoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("tipoServicoId", "Tipo de serviço não encontrado."));
        if (!tipo.isAtivo()) {
            throw new RegraNegocioException("tipoServicoId",
                    "O tipo de serviço '" + tipo.getNome() + "' está inativo e não pode ser usado em novos registros.");
        }
        return tipo;
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
}
