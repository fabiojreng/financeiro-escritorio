package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.BeneficioConcedido;
import br.com.escritorio.financeiro.domain.Cpf;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.ALTURA_LINHA;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.LINHA_CABECALHO;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.ajustar;
import static br.com.escritorio.financeiro.web.LeiauteImpressaoExcel.texto;

/**
 * Planilha Excel dos benefícios já concedidos, no leiaute de impressão comum
 * ({@link LeiauteImpressaoExcel}). Sai o que a lista está mostrando: o mês e os filtros aplicados.
 */
final class BeneficiosConcedidosExcel {

    static final String ABA = "Benefícios concedidos";
    static final String TABELA = "BeneficiosConcedidos";
    static final List<String> CABECALHO = List.of("CLIENTE", "CPF", "BENEFÍCIO", "RESPONSÁVEL");

    /** Acima destas larguras (em caracteres) o texto quebra em mais linhas em vez de ser cortado. */
    private static final int[] LARGURAS_MAXIMAS = {48, 19, 42, 30};

    private BeneficiosConcedidosExcel() {
    }

    static byte[] gerar(String tituloPeriodo, List<BeneficioConcedido> beneficios) {
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            XSSFSheet aba = planilha.createSheet(ABA);
            LeiauteImpressaoExcel.Estilos estilos = new LeiauteImpressaoExcel.Estilos(planilha, "#,##0.00");

            LeiauteImpressaoExcel.escreverCabecalho(aba, CABECALHO, estilos.cabecalho);

            int[] larguras = larguras(beneficios);
            int linha = LINHA_CABECALHO + 1;
            for (BeneficioConcedido beneficio : beneficios) {
                Row row = aba.createRow(linha++);
                String cliente = beneficio.getCliente().getNome();
                String tipo = beneficio.getTipoServico().getNome();
                String responsavel = beneficio.getResponsavel().getNome();
                row.setHeightInPoints(ALTURA_LINHA * LeiauteImpressaoExcel.linhasNecessarias(larguras,
                        cliente, null, tipo, responsavel));
                texto(row, 0, cliente, estilos.texto);
                texto(row, 1, Cpf.formatar(beneficio.getCliente().getCpf()), estilos.texto);
                texto(row, 2, tipo, estilos.texto);
                texto(row, 3, responsavel, estilos.texto);
            }

            Row total = aba.createRow(linha);
            total.setHeightInPoints(ALTURA_LINHA);
            texto(total, 0, rotuloTotal(beneficios.size()), estilos.totalRotulo);
            for (int i = 1; i < CABECALHO.size(); i++) {
                total.createCell(i).setCellStyle(estilos.totalRotulo);
            }
            aba.addMergedRegion(new CellRangeAddress(linha, linha, 0, CABECALHO.size() - 1));

            LeiauteImpressaoExcel.aplicarLarguras(aba, larguras);
            aba.createFreezePane(0, 1);
            if (!beneficios.isEmpty()) {
                LeiauteImpressaoExcel.criarTabela(aba, TABELA, CABECALHO.size(), linha - 1);
            }
            String periodo = tituloPeriodo == null || tituloPeriodo.isBlank()
                    ? "Benefícios concedidos" : tituloPeriodo;
            LeiauteImpressaoExcel.configurarImpressao(planilha, aba, "Benefícios concedidos — " + periodo,
                    CABECALHO.size(), linha);

            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar a planilha de benefícios concedidos.", e);
        }
    }

    /** Largura de cada coluna pelo maior conteúdo, limitada à máxima, mas nunca menor que o cabeçalho. */
    static int[] larguras(List<BeneficioConcedido> beneficios) {
        int[] larguras = new int[CABECALHO.size()];
        for (BeneficioConcedido beneficio : beneficios) {
            ajustar(larguras, 0, beneficio.getCliente().getNome());
            ajustar(larguras, 1, Cpf.formatar(beneficio.getCliente().getCpf()));
            ajustar(larguras, 2, beneficio.getTipoServico().getNome());
            ajustar(larguras, 3, beneficio.getResponsavel().getNome());
        }
        return LeiauteImpressaoExcel.limitar(larguras, CABECALHO, LARGURAS_MAXIMAS);
    }

    private static String rotuloTotal(int quantidade) {
        return quantidade == 1 ? "Total (1 benefício)" : "Total (" + quantidade + " benefícios)";
    }
}
