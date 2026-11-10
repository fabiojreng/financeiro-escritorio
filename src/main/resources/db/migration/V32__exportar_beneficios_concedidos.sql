-- Baixar em Excel a lista mensal de benefícios concedidos, como já existe para perícias e pagamentos.
insert into perfil_permissao (perfil_id, permissao)
select p.id, 'BENEFICIOS_CONCEDIDOS_EXPORTAR'
from perfil p
where p.nome in ('Administrador', 'Operador');
