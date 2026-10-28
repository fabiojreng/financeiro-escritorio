-- Repasse pago em partes: na situação "Parcial" fica guardado quanto o responsável já recebeu.
alter table pagamento_responsavel
    add column valor_repasse_pago numeric(15, 2),
    drop constraint ck_pagamento_responsavel_situacao_repasse,
    add constraint ck_pagamento_responsavel_situacao_repasse
        check (situacao_repasse in ('PENDENTE', 'PARCIAL', 'PAGO'));

-- Exportar a agenda de perícias em Excel, como já existe para a agenda de pagamentos.
insert into perfil_permissao (perfil_id, permissao)
select p.id, 'PERICIAS_EXPORTAR'
from perfil p
where p.nome in ('Administrador', 'Operador');
