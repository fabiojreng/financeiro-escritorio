-- Exclusão de pagamentos agendados e de perícias lançados por engano ou em duplicidade.
-- O registro é apagado; os dados e o motivo ficam na auditoria.
insert into perfil_permissao (perfil_id, permissao)
select p.id, x.permissao
from perfil p
cross join (values ('AGENDA_EXCLUIR'), ('PERICIAS_EXCLUIR')) as x (permissao)
where p.nome in ('Administrador', 'Operador');
