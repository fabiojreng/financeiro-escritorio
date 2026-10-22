-- O escritório não empresta dinheiro aos funcionários: sai o controle de empréstimos criado na V23.
-- Parcelas já lançadas viram descontos com a mesma descrição, e nos acertos pagos o total das parcelas
-- passa para os descontos, para que o líquido gravado continue batendo com os totais.

update lancamento_funcionario
set tipo      = 'DESCONTO',
    descricao = coalesce(descricao, 'Parcela de empréstimo')
where tipo = 'PARCELA_EMPRESTIMO';

alter table lancamento_funcionario drop constraint ck_lancamento_funcionario_emprestimo;
alter table lancamento_funcionario drop constraint ck_lancamento_funcionario_tipo;
alter table lancamento_funcionario drop column emprestimo_id;
alter table lancamento_funcionario drop column numero_parcela;
alter table lancamento_funcionario add constraint ck_lancamento_funcionario_tipo
    check (tipo in ('VALE', 'DESCONTO', 'BONIFICACAO', 'HORA_EXTRA', 'REEMBOLSO'));

drop table emprestimo_funcionario;

update fechamento_funcionario
set total_descontos = total_descontos + total_parcelas
where total_parcelas is not null;

alter table fechamento_funcionario drop constraint ck_fechamento_funcionario_pagamento;
alter table fechamento_funcionario drop column total_parcelas;
alter table fechamento_funcionario add constraint ck_fechamento_funcionario_pagamento
    check (not pago or (data_pagamento is not null and forma_pagamento is not null and pago_em is not null
                        and pago_por is not null and salario_base is not null and total_bonificacoes is not null
                        and total_reembolsos is not null and total_vales is not null
                        and total_descontos is not null and liquido is not null));
