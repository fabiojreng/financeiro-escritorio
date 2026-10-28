package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Cpf;
import br.com.escritorio.financeiro.domain.Pericia;
import br.com.escritorio.financeiro.domain.SituacaoPericia;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.ALTURA_LINHA;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.LINHA_CABECALHO;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.ajustar;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.data;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.texto;

/**
 * Planilha Excel da agenda de perícias, para imprimir e levar no dia, no leiaute de impressão comum
 * ({@link LeiauteImpressaoExcel}). Só entram as perícias com data no período; as "a agendar" ficam de fora.
 */
final class AgendaPericiasExcel {

    static final String ABA = "Perícias";
    static final String TABELA = "Pericias";
    static final List<String> CABECALHO = List.of(
            "DATA", "HORÁRIO", "CLIENTE", "CPF", "NOME DA MÃE", "TIPO", "DE QUEM É", "LOCAL", "SITUAÇÃO",
            "OBSERVAÇÕES");
    static final String LOCAL_A_DEFINIR = "A definir";

    private static final int COLUNA_SITUACAO = 8;
    /** Acima destas larguras (em caracteres) o texto quebra em mais linhas em vez de ser cortado. */
    private static final int[] LARGURAS_MAXIMAS = {14, 12, 40, 19, 36, 32, 24, 30, 18, 50};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private AgendaPericiasExcel() {
    }

    static byte[] gerar(String tituloPeriodo, List<Pericia> pericias) {
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            XSSFSheet aba = planilha.createSheet(ABA);
            Estilos estilos = new Estilos(planilha);

            LeiauteImpressaoExcel.escreverCabecalho(aba, CABECALHO, estilos.base.cabecalho);

            int[] larguras = larguras(pericias);
            int linha = LINHA_CABECALHO + 1;
            for (Pericia pericia : pericias) {
                Row row = aba.createRow(linha++);
                String cliente = pericia.getCliente().getNome();
                String mae = pericia.getCliente().getNomeMae();
                String tipo = pericia.getTipoPericia().getNome();
                String responsavel = pericia.getResponsavel().getNome();
                String local = local(pericia);
                row.setHeightInPoints(ALTURA_LINHA * LeiauteImpressaoExcel.linhasNecessarias(larguras, null, null,
                        cliente, null, mae, tipo, responsavel, local, null, pericia.getObservacoes()));
                CellStyle texto = estilos.base.texto;
                data(row, 0, pericia.getData(), estilos.base.data);
                texto(row, 1, horario(pericia), texto);
                texto(row, 2, cliente, texto);
                texto(row, 3, Cpf.formatar(pericia.getCliente().getCpf()), texto);
                texto(row, 4, mae, texto);
                texto(row, 5, tipo, texto);
                texto(row, 6, responsavel, texto);
                texto(row, 7, local, texto);
                texto(row, COLUNA_SITUACAO, pericia.getSituacao().getRotulo(),
                        estilos.situacoes.get(pericia.getSituacao()));
                texto(row, 9, pericia.getObservacoes(), texto);
            }

            Row total = aba.createRow(linha);
            total.setHeightInPoints(ALTURA_LINHA);
            texto(total, 0, rotuloTotal(pericias.size()), estilos.base.totalRotulo);
            for (int i = 1; i < CABECALHO.size(); i++) {
                total.createCell(i).setCellStyle(estilos.base.totalRotulo);
            }
            // Data e horário são estreitas demais para o rótulo do total.
            aba.addMergedRegion(new CellRangeAddress(linha, linha, 0, 2));

            LeiauteImpressaoExcel.aplicarLarguras(aba, larguras);
            aba.createFreezePane(0, 1);
            if (!pericias.isEmpty()) {
                LeiauteImpressaoExcel.criarTabela(aba, TABELA, CABECALHO.size(), linha - 1);
            }
            String periodo = tituloPeriodo == null || tituloPeriodo.isBlank() ? "Agenda de perícias" : tituloPeriodo;
            LeiauteImpressaoExcel.configurarImpressao(planilha, aba, "Agenda de perícias — " + periodo,
                    CABECALHO.size(), linha);

            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar a agenda de perícias.", e);
        }
    }

    /** Largura de cada coluna pelo maior conteúdo, limitada à máxima, mas nunca menor que o cabeçalho. */
    static int[] larguras(List<Pericia> pericias) {
        int[] larguras = new int[CABECALHO.size()];
        for (Pericia p : pericias) {
            ajustar(larguras, 0, "00/00/0000");
            ajustar(larguras, 1, horario(p));
            ajustar(larguras, 2, p.getCliente().getNome());
            ajustar(larguras, 3, Cpf.formatar(p.getCliente().getCpf()));
            ajustar(larguras, 4, p.getCliente().getNomeMae());
            ajustar(larguras, 5, p.getTipoPericia().getNome());
            ajustar(larguras, 6, p.getResponsavel().getNome());
            ajustar(larguras, 7, local(p));
            ajustar(larguras, COLUNA_SITUACAO, p.getSituacao().getRotulo());
            ajustar(larguras, 9, p.getObservacoes());
        }
        return LeiauteImpressaoExcel.limitar(larguras, CABECALHO, LARGURAS_MAXIMAS);
    }

    private static String horario(Pericia pericia) {
        return pericia.getHorario() == null ? "—" : HORA.format(pericia.getHorario());
    }

    private static String local(Pericia pericia) {
        return pericia.getLocal() == null ? LOCAL_A_DEFINIR : pericia.getLocal().getNome();
    }

    private static String rotuloTotal(int quantidade) {
        return quantidade == 1 ? "Total (1 perícia)" : "Total (" + quantidade + " perícias)";
    }

    private static final class Estilos {

        final LeiauteImpressaoExcel.Estilos base;
        final Map<SituacaoPericia, CellStyle> situacoes = new EnumMap<>(SituacaoPericia.class);

        Estilos(XSSFWorkbook planilha) {
            base = new LeiauteImpressaoExcel.Estilos(planilha, "#,##0.00");
            situacoes.put(SituacaoPericia.AGENDADA, base.destaque("DDEBF7", "1F4E78"));
            situacoes.put(SituacaoPericia.REALIZADA, base.destaque("C6EFCE", "006100"));
            situacoes.put(SituacaoPericia.FALTOU, base.destaque("FFC7CE", "9C0006"));
            situacoes.put(SituacaoPericia.REMARCADA, base.destaque("FFEB9C", "9C5700"));
            situacoes.put(SituacaoPericia.CANCELADA, base.destaque("EDEDED", "595959"));
        }
    }
}
