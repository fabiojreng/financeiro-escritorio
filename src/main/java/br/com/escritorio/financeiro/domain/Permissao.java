package br.com.escritorio.financeiro.domain;

/**
 * Ação do sistema que pode ser liberada para um perfil. O nome da constante é a authority verificada pelo
 * Spring Security ({@code hasAuthority('DESPESAS_REABRIR_FECHAMENTO')}) e é gravado na tabela perfil_permissao:
 * renomear uma constante exige migration.
 */
public enum Permissao {

    CLIENTES_VISUALIZAR(ModuloPermissao.CLIENTES, "Visualizar clientes",
            "Consultar a lista e a ficha dos clientes."),
    CLIENTES_EDITAR(ModuloPermissao.CLIENTES, "Cadastrar e editar clientes",
            "Cadastrar clientes novos e alterar os dados dos existentes."),
    CLIENTES_INATIVAR(ModuloPermissao.CLIENTES, "Inativar clientes",
            "Inativar e reativar clientes."),

    CADASTROS_RESPONSAVEIS(ModuloPermissao.CADASTROS, "Gerenciar responsáveis",
            "Cadastrar, editar, inativar e reativar responsáveis."),
    CADASTROS_TIPOS_SERVICO(ModuloPermissao.CADASTROS, "Gerenciar tipos de serviço",
            "Cadastrar, editar, inativar e reativar tipos de serviço."),

    AGENDA_VISUALIZAR(ModuloPermissao.AGENDA, "Visualizar a agenda",
            "Consultar a agenda de pagamentos."),
    AGENDA_EDITAR(ModuloPermissao.AGENDA, "Lançar e editar pagamentos",
            "Lançar pagamentos na agenda e alterar os existentes."),
    AGENDA_ALTERAR_STATUS(ModuloPermissao.AGENDA, "Alterar status",
            "Mudar o status dos pagamentos direto na agenda."),
    AGENDA_EXPORTAR(ModuloPermissao.AGENDA, "Exportar Excel",
            "Baixar a agenda de pagamentos em planilha."),

    HONORARIOS_VISUALIZAR(ModuloPermissao.HONORARIOS, "Visualizar honorários",
            "Consultar os honorários e o detalhe de cada um."),
    HONORARIOS_EDITAR(ModuloPermissao.HONORARIOS, "Lançar e editar honorários",
            "Lançar honorários, negociar parcelas, registrar e estornar recebimentos."),

    REPASSES_VISUALIZAR(ModuloPermissao.REPASSES, "Visualizar repasses",
            "Consultar os repasses e ver os valores repassados aos responsáveis."),
    REPASSES_EDITAR(ModuloPermissao.REPASSES, "Lançar e editar repasses",
            "Informar o valor e a situação dos repasses."),

    DESPESAS_VISUALIZAR(ModuloPermissao.DESPESAS, "Visualizar despesas",
            "Consultar as despesas do escritório e baixar a planilha de contas pagas."),
    DESPESAS_EDITAR(ModuloPermissao.DESPESAS, "Lançar e editar despesas",
            "Lançar despesas novas e alterar as existentes."),
    DESPESAS_CANCELAR(ModuloPermissao.DESPESAS, "Cancelar despesas",
            "Cancelar despesas lançadas."),
    DESPESAS_CADASTROS(ModuloPermissao.DESPESAS, "Gerenciar categorias e formas de pagamento",
            "Cadastrar, editar, inativar e reativar categorias de despesa e formas de pagamento."),
    DESPESAS_ENVIAR_FECHAMENTO(ModuloPermissao.DESPESAS, "Enviar fechamento",
            "Marcar a planilha do mês como enviada à contabilidade, travando as despesas do mês."),
    DESPESAS_REABRIR_FECHAMENTO(ModuloPermissao.DESPESAS, "Reabrir fechamento",
            "Reabrir um mês já enviado à contabilidade para corrigir despesas."),

    FUNCIONARIOS_VISUALIZAR(ModuloPermissao.FUNCIONARIOS, "Visualizar funcionários",
            "Consultar o cadastro, os lançamentos, os empréstimos e o acerto mensal dos funcionários."),
    FUNCIONARIOS_EDITAR(ModuloPermissao.FUNCIONARIOS, "Cadastrar e lançar",
            "Cadastrar e inativar funcionários, lançar vales, descontos, extras, reembolsos e empréstimos."),
    FUNCIONARIOS_CANCELAR(ModuloPermissao.FUNCIONARIOS, "Cancelar lançamentos",
            "Cancelar lançamentos e empréstimos dos funcionários."),
    FUNCIONARIOS_FECHAR(ModuloPermissao.FUNCIONARIOS, "Pagar o acerto mensal",
            "Marcar o acerto do mês como pago, fechando a competência do funcionário."),
    FUNCIONARIOS_REABRIR(ModuloPermissao.FUNCIONARIOS, "Reabrir acerto",
            "Reabrir um acerto já pago para corrigir os lançamentos do mês."),
    FUNCIONARIOS_RELATORIOS(ModuloPermissao.FUNCIONARIOS, "Relatórios de funcionários",
            "Consultar e exportar extratos, vales, empréstimos em aberto e o total gasto por mês."),

    RELATORIOS_VISUALIZAR(ModuloPermissao.RELATORIOS, "Visualizar relatórios",
            "Consultar e exportar os relatórios financeiros e o histórico dos clientes."),

    ADMIN_USUARIOS(ModuloPermissao.ADMINISTRACAO, "Gerenciar usuários",
            "Cadastrar, editar, ativar e desativar usuários e definir o perfil de cada um."),
    ADMIN_PERFIS(ModuloPermissao.ADMINISTRACAO, "Gerenciar perfis",
            "Criar, editar, duplicar e inativar perfis e escolher as permissões de cada um."),
    ADMIN_AUDITORIA(ModuloPermissao.ADMINISTRACAO, "Visualizar auditoria",
            "Consultar o histórico de operações feitas no sistema.");

    private final ModuloPermissao modulo;
    private final String rotulo;
    private final String descricao;

    Permissao(ModuloPermissao modulo, String rotulo, String descricao) {
        this.modulo = modulo;
        this.rotulo = rotulo;
        this.descricao = descricao;
    }

    public ModuloPermissao getModulo() {
        return modulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDescricao() {
        return descricao;
    }
}
