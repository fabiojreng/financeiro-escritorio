-- O caderno nem sempre indica quem acompanhou o benefício. O responsável deixa de ser obrigatório.
alter table beneficio_concedido alter column responsavel_id drop not null;
