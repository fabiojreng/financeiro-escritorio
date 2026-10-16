document.addEventListener('submit', function (evento) {
    var mensagem = evento.target.getAttribute('data-confirmar');
    if (mensagem && !window.confirm(mensagem)) {
        evento.preventDefault();
    }
});

// Elementos com data-mostrar-quando="idDoCampo=VALOR" só aparecem quando o campo tem esse valor;
// com "idDoCampo!=VALOR", só quando o campo tem qualquer outro valor.
function atualizarCamposCondicionais() {
    document.querySelectorAll('[data-mostrar-quando]').forEach(function (elemento) {
        var regra = elemento.getAttribute('data-mostrar-quando');
        var negada = regra.indexOf('!=') >= 0;
        var partes = regra.split(negada ? '!=' : '=');
        var campo = document.getElementById(partes[0]);
        if (campo) {
            elemento.hidden = (campo.value === partes[1]) === negada;
        }
    });
}

document.addEventListener('change', atualizarCamposCondicionais);

// Campos com data-enviar-ao-mudar salvam o formulário assim que o valor muda (ex.: status na linha da agenda).
document.addEventListener('change', function (evento) {
    if (evento.target.hasAttribute('data-enviar-ao-mudar')) {
        evento.target.form.requestSubmit();
    }
});
document.addEventListener('DOMContentLoaded', atualizarCamposCondicionais);

// Um select com data-preencher="idDoCampo" copia o data-texto da opção escolhida para o campo,
// desde que o campo esteja vazio ou ainda com o texto sugerido antes (o que foi digitado não é apagado).
document.addEventListener('change', function (evento) {
    var select = evento.target;
    var idCampo = select.getAttribute && select.getAttribute('data-preencher');
    if (!idCampo) {
        return;
    }
    var campo = document.getElementById(idCampo);
    var opcao = select.options[select.selectedIndex];
    var texto = opcao ? opcao.getAttribute('data-texto') : null;
    if (!campo || !texto) {
        return;
    }
    var sugeridoAntes = campo.getAttribute('data-sugerido');
    if (campo.value.trim() === '' || campo.value === sugeridoAntes) {
        campo.value = texto;
        campo.setAttribute('data-sugerido', texto);
    }
});

document.addEventListener('click', function (evento) {
    if (evento.target.closest('[data-imprimir]')) {
        window.print();
    }
});

// Formulários com data-cadastro-rapido="idDoSelect" ficam numa janela (modal) dentro de outro formulário:
// cadastram sem recarregar a página, para não perder o que já foi digitado, e selecionam o item criado.
document.addEventListener('submit', function (evento) {
    var formulario = evento.target;
    var idSelect = formulario.getAttribute('data-cadastro-rapido');
    if (!idSelect) {
        return;
    }
    evento.preventDefault();
    var caixaErro = formulario.querySelector('[data-erro]');
    var botao = formulario.querySelector('[type=submit]');
    caixaErro.hidden = true;
    botao.disabled = true;

    fetch(formulario.action, {
        method: 'POST',
        body: new URLSearchParams(new FormData(formulario)),
        headers: {'Accept': 'application/json'}
    })
        .then(function (resposta) {
            var tipo = resposta.headers.get('Content-Type') || '';
            if (tipo.indexOf('application/json') < 0) {
                throw new Error('Não foi possível cadastrar. Sua sessão pode ter expirado: recarregue a página e tente de novo.');
            }
            return resposta.json();
        })
        .then(function (dados) {
            if (dados.erro) {
                throw new Error(dados.erro);
            }
            var select = document.getElementById(idSelect);
            var opcao = new Option(dados.nome, dados.id, true, true);
            if (dados.texto) {
                opcao.setAttribute('data-texto', dados.texto);
            }
            select.add(opcao);
            select.classList.remove('is-invalid');
            select.dispatchEvent(new Event('change', {bubbles: true}));
            formulario.reset();
            bootstrap.Modal.getOrCreateInstance(formulario.closest('.modal')).hide();
        })
        .catch(function (erro) {
            caixaErro.textContent = erro instanceof TypeError
                ? 'Não foi possível falar com o servidor. Verifique a conexão e tente de novo.'
                : erro.message;
            caixaErro.hidden = false;
        })
        .finally(function () {
            botao.disabled = false;
        });
});

// Bloco com data-busca-cliente: o campo de nome sugere clientes cadastrados enquanto se digita (nome ou CPF).
// Escolher uma sugestão liga o pagamento ao cliente, sem digitar tudo de novo; "Trocar cliente" desfaz.
document.querySelectorAll('[data-busca-cliente]').forEach(function (bloco) {
    var urlSugestoes = bloco.getAttribute('data-url-sugestoes');
    var urlFicha = bloco.getAttribute('data-url-ficha');
    var proximoCampo = document.getElementById(bloco.getAttribute('data-proximo-campo'));
    var campoId = bloco.querySelector('[data-cliente-id]');
    var campoNome = bloco.querySelector('[data-cliente-nome]');
    var campoCpf = bloco.querySelector('[data-cliente-cpf]');
    var lista = bloco.querySelector('[data-sugestoes]');
    var avisoEscolhido = bloco.querySelector('[data-cliente-escolhido]');
    var avisoSemCpf = bloco.querySelector('[data-aviso-sem-cpf]');
    var linkFicha = bloco.querySelector('[data-link-ficha]');
    var ajudaNovo = bloco.querySelector('[data-ajuda-cliente-novo]');
    var avisoCpf = bloco.querySelector('[data-cpf-cadastrado]');
    var nomeCpfCadastrado = bloco.querySelector('[data-nome-cpf-cadastrado]');
    var sugestoes = [];
    var destacada = -1;
    var espera = null;
    var ultimaBusca = 0;
    var clienteDoCpf = null;

    function buscar(termo) {
        return fetch(urlSugestoes + '?termo=' + encodeURIComponent(termo), {headers: {'Accept': 'application/json'}})
            .then(function (resposta) {
                var tipo = resposta.headers.get('Content-Type') || '';
                if (!resposta.ok || tipo.indexOf('application/json') < 0) {
                    throw new Error('Busca de clientes indisponível.');
                }
                return resposta.json();
            });
    }

    function fecharLista() {
        lista.hidden = true;
        lista.replaceChildren();
        campoNome.setAttribute('aria-expanded', 'false');
        campoNome.removeAttribute('aria-activedescendant');
        sugestoes = [];
        destacada = -1;
    }

    function mostrarSugestoes(clientes) {
        lista.replaceChildren();
        sugestoes = clientes;
        destacada = -1;
        if (clientes.length === 0) {
            var vazio = document.createElement('div');
            vazio.className = 'list-group-item small text-body-secondary';
            vazio.textContent = 'Nenhum cliente cadastrado encontrado. Preencha o CPF para cadastrar.';
            lista.appendChild(vazio);
        }
        clientes.forEach(function (cliente, indice) {
            var item = document.createElement('button');
            item.type = 'button';
            item.id = 'sugestao-cliente-' + indice;
            item.className = 'list-group-item list-group-item-action';
            item.tabIndex = -1;
            item.setAttribute('role', 'option');
            item.setAttribute('aria-selected', 'false');
            var nome = document.createElement('div');
            nome.className = 'fw-semibold';
            nome.textContent = cliente.nome;
            var detalhe = document.createElement('div');
            detalhe.className = 'small text-body-secondary';
            detalhe.textContent = (cliente.cpf ? 'CPF ' + cliente.cpf : 'Sem CPF cadastrado')
                + (cliente.telefone ? ' · ' + cliente.telefone : '');
            item.append(nome, detalhe);
            item.addEventListener('mousedown', function (evento) {
                evento.preventDefault();
            });
            item.addEventListener('click', function () {
                escolher(cliente);
            });
            lista.appendChild(item);
        });
        lista.hidden = false;
        campoNome.setAttribute('aria-expanded', 'true');
    }

    function destacar(indice) {
        var itens = lista.querySelectorAll('[role=option]');
        if (itens.length === 0) {
            return;
        }
        destacada = (indice + itens.length) % itens.length;
        itens.forEach(function (item, i) {
            item.classList.toggle('active', i === destacada);
            item.setAttribute('aria-selected', i === destacada ? 'true' : 'false');
        });
        campoNome.setAttribute('aria-activedescendant', itens[destacada].id);
        itens[destacada].scrollIntoView({block: 'nearest'});
    }

    function escolher(cliente) {
        campoId.value = cliente.id;
        campoNome.value = cliente.nome;
        campoNome.readOnly = true;
        campoCpf.value = cliente.cpf || '';
        campoCpf.readOnly = Boolean(cliente.cpf);
        campoNome.classList.remove('is-invalid');
        campoCpf.classList.remove('is-invalid');
        linkFicha.href = urlFicha + cliente.id;
        avisoSemCpf.hidden = Boolean(cliente.cpf);
        avisoEscolhido.hidden = false;
        ajudaNovo.hidden = true;
        avisoCpf.hidden = true;
        fecharLista();
        (cliente.cpf && proximoCampo ? proximoCampo : campoCpf).focus();
    }

    function trocar() {
        campoId.value = '';
        campoNome.value = '';
        campoCpf.value = '';
        campoNome.readOnly = false;
        campoCpf.readOnly = false;
        avisoEscolhido.hidden = true;
        ajudaNovo.hidden = false;
        campoNome.focus();
    }

    campoNome.addEventListener('input', function () {
        clearTimeout(espera);
        var termo = campoNome.value.trim();
        if (termo.length < 2) {
            fecharLista();
            return;
        }
        espera = setTimeout(function () {
            var busca = ++ultimaBusca;
            buscar(termo)
                .then(function (clientes) {
                    if (busca === ultimaBusca && document.activeElement === campoNome) {
                        mostrarSugestoes(clientes);
                    }
                })
                .catch(fecharLista);
        }, 250);
    });

    campoNome.addEventListener('keydown', function (evento) {
        if (lista.hidden) {
            return;
        }
        if (evento.key === 'ArrowDown') {
            evento.preventDefault();
            destacar(destacada + 1);
        } else if (evento.key === 'ArrowUp') {
            evento.preventDefault();
            destacar(destacada < 0 ? -1 : destacada - 1);
        } else if (evento.key === 'Enter' && destacada >= 0) {
            evento.preventDefault();
            escolher(sugestoes[destacada]);
        } else if (evento.key === 'Escape') {
            fecharLista();
        }
    });

    campoNome.addEventListener('blur', fecharLista);

    // CPF digitado à mão que já é de um cliente: oferece usar o cadastro em vez de esperar o erro ao salvar.
    campoCpf.addEventListener('change', function () {
        avisoCpf.hidden = true;
        clienteDoCpf = null;
        var digitos = campoCpf.value.replace(/\D/g, '');
        if (campoId.value || digitos.length !== 11) {
            return;
        }
        buscar(digitos)
            .then(function (clientes) {
                clienteDoCpf = clientes.find(function (cliente) {
                    return cliente.cpf && cliente.cpf.replace(/\D/g, '') === digitos;
                }) || null;
                if (clienteDoCpf && !campoId.value) {
                    nomeCpfCadastrado.textContent = clienteDoCpf.nome;
                    avisoCpf.hidden = false;
                }
            })
            .catch(function () {
            });
    });

    bloco.querySelector('[data-usar-cpf-cadastrado]').addEventListener('click', function () {
        if (clienteDoCpf) {
            escolher(clienteDoCpf);
        }
    });

    bloco.querySelector('[data-trocar-cliente]').addEventListener('click', trocar);
});

document.addEventListener('shown.bs.modal', function (evento) {
    var formulario = evento.target.querySelector('[data-cadastro-rapido]');
    if (formulario) {
        formulario.querySelector('[data-erro]').hidden = true;
        var primeiroCampo = formulario.querySelector('input:not([type=hidden]), textarea');
        if (primeiroCampo) {
            primeiroCampo.focus();
        }
    }
});
