document.addEventListener('submit', function (evento) {
    var mensagem = evento.target.getAttribute('data-confirmar');
    if (mensagem && !window.confirm(mensagem)) {
        evento.preventDefault();
    }
});

// Elementos com data-mostrar-quando="idDoCampo=VALOR" só aparecem quando o campo tem esse valor;
// com "idDoCampo!=VALOR", só quando o campo tem qualquer outro valor. Vários valores vão separados por "|".
function atualizarCamposCondicionais() {
    document.querySelectorAll('[data-mostrar-quando]').forEach(function (elemento) {
        var regra = elemento.getAttribute('data-mostrar-quando');
        var negada = regra.indexOf('!=') >= 0;
        var partes = regra.split(negada ? '!=' : '=');
        var campo = document.getElementById(partes[0]);
        if (campo) {
            elemento.hidden = (partes[1].split('|').indexOf(campo.value) >= 0) === negada;
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

// Select de situação (.select-situacao) troca de cor junto com a opção escolhida (data-classe da opção).
document.addEventListener('change', function (evento) {
    var select = evento.target;
    if (!select.classList || !select.classList.contains('select-situacao')) {
        return;
    }
    Array.from(select.classList)
        .filter(function (classe) { return classe.indexOf('selo-') === 0; })
        .forEach(function (classe) { select.classList.remove(classe); });
    var classe = select.selectedOptions[0] && select.selectedOptions[0].getAttribute('data-classe');
    if (classe) {
        select.classList.add(classe);
    }
});

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

// Linhas com data-link abrem o registro ao clicar em qualquer parte da linha, exceto nos botões, links e campos dela.
document.addEventListener('click', function (evento) {
    var linha = evento.target.closest('tr[data-link]');
    if (!linha || evento.target.closest('a, button, input, select, textarea, label, form')) {
        return;
    }
    if (window.getSelection && String(window.getSelection()).length > 0) {
        return;
    }
    iniciarCarregamento();
    window.location.href = linha.getAttribute('data-link');
});

// Uma caixa com data-marcar-todos="GRUPO" marca ou desmarca as caixas com data-grupo="GRUPO" e fica
// marcada (ou indeterminada) conforme elas.
function atualizarMarcarTodos(grupo) {
    var mestre = document.querySelector('[data-marcar-todos="' + grupo + '"]');
    if (!mestre) {
        return;
    }
    var caixas = Array.from(document.querySelectorAll('[data-grupo="' + grupo + '"]'));
    var marcadas = caixas.filter(function (caixa) { return caixa.checked; }).length;
    mestre.checked = caixas.length > 0 && marcadas === caixas.length;
    mestre.indeterminate = marcadas > 0 && marcadas < caixas.length;
}

document.addEventListener('change', function (evento) {
    var alvo = evento.target;
    var grupoMestre = alvo.getAttribute && alvo.getAttribute('data-marcar-todos');
    if (grupoMestre) {
        document.querySelectorAll('[data-grupo="' + grupoMestre + '"]').forEach(function (caixa) {
            if (!caixa.disabled) {
                caixa.checked = alvo.checked;
            }
        });
        return;
    }
    var grupo = alvo.getAttribute && alvo.getAttribute('data-grupo');
    if (grupo) {
        atualizarMarcarTodos(grupo);
    }
});

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-marcar-todos]').forEach(function (mestre) {
        atualizarMarcarTodos(mestre.getAttribute('data-marcar-todos'));
    });
});

// Avisos de sucesso somem sozinhos depois de alguns segundos; os de erro ficam até serem fechados.
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-fechar-sozinho]').forEach(function (aviso) {
        setTimeout(function () {
            bootstrap.Alert.getOrCreateInstance(aviso).close();
        }, 6000);
    });
});

// Lista de caixas de marcar (data-lista-marcavel="nomeDoCampo"): o item cadastrado entra já marcado.
function adicionarItemMarcado(lista, dados) {
    var nomeCampo = lista.getAttribute('data-lista-marcavel');
    var idCaixa = nomeCampo + '-' + dados.id;
    var item = document.createElement('div');
    item.className = 'form-check';
    var caixa = document.createElement('input');
    caixa.type = 'checkbox';
    caixa.className = 'form-check-input';
    caixa.name = nomeCampo;
    caixa.value = dados.id;
    caixa.id = idCaixa;
    caixa.checked = true;
    var rotulo = document.createElement('label');
    rotulo.className = 'form-check-label';
    rotulo.htmlFor = idCaixa;
    rotulo.textContent = dados.nome;
    item.append(caixa, rotulo);
    var avisoVazio = lista.querySelector('[data-lista-vazia]');
    if (avisoVazio) {
        avisoVazio.remove();
    }
    lista.append(item);
    lista.classList.remove('is-invalid');
    item.scrollIntoView({block: 'nearest'});
    caixa.dispatchEvent(new Event('change', {bubbles: true}));
}

// Formulários com data-cadastro-rapido="idDoCampo" ficam numa janela (modal) dentro de outro formulário:
// cadastram sem recarregar a página, para não perder o que já foi digitado, e selecionam (ou marcam) o item criado.
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
            var destino = document.getElementById(idSelect);
            if (destino.hasAttribute('data-lista-marcavel')) {
                adicionarItemMarcado(destino, dados);
            } else {
                var opcao = new Option(dados.nome, dados.id, true, true);
                if (dados.texto) {
                    opcao.setAttribute('data-texto', dados.texto);
                }
                destino.add(opcao);
                destino.classList.remove('is-invalid');
                destino.dispatchEvent(new Event('change', {bubbles: true}));
            }
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

// Sugestões de clientes (nome ou CPF) vindas de /clientes/sugestoes; a URL pode já trazer parâmetros (ex.: todos=true).
function buscarSugestoesCliente(url, termo) {
    var endereco = new URL(url, window.location.origin);
    endereco.searchParams.set('termo', termo);
    return fetch(endereco, {headers: {'Accept': 'application/json'}})
        .then(function (resposta) {
            var tipo = resposta.headers.get('Content-Type') || '';
            if (!resposta.ok || tipo.indexOf('application/json') < 0) {
                throw new Error('Busca de clientes indisponível.');
            }
            return resposta.json();
        });
}

// Lista de sugestões de clientes presa a um campo de texto: mostra nome, CPF e se está inativo; as setas
// destacam, Enter escolhe a destacada (sem destaque, Enter segue o normal do formulário) e Esc fecha.
function criarListaSugestoesCliente(campo, lista, opcoes) {
    var sugestoes = [];
    var destacada = -1;

    function fechar() {
        lista.hidden = true;
        lista.replaceChildren();
        campo.setAttribute('aria-expanded', 'false');
        campo.removeAttribute('aria-activedescendant');
        sugestoes = [];
        destacada = -1;
    }

    function mostrar(clientes) {
        lista.replaceChildren();
        sugestoes = clientes;
        destacada = -1;
        if (clientes.length === 0) {
            var vazio = document.createElement('div');
            vazio.className = 'list-group-item small text-body-secondary';
            vazio.textContent = opcoes.mensagemVazia;
            lista.appendChild(vazio);
        }
        clientes.forEach(function (cliente, indice) {
            var item = document.createElement('button');
            item.type = 'button';
            item.id = lista.id + '-' + indice;
            item.className = 'list-group-item list-group-item-action';
            item.tabIndex = -1;
            item.setAttribute('role', 'option');
            item.setAttribute('aria-selected', 'false');
            var nome = document.createElement('div');
            nome.className = 'fw-semibold';
            nome.textContent = cliente.nome;
            if (cliente.ativo === false) {
                var selo = document.createElement('span');
                selo.className = 'badge selo-neutro ms-2';
                selo.textContent = 'Inativo';
                nome.appendChild(selo);
            }
            var detalhe = document.createElement('div');
            detalhe.className = 'small text-body-secondary';
            detalhe.textContent = (cliente.cpf ? 'CPF ' + cliente.cpf : 'Sem CPF cadastrado')
                + (cliente.telefone ? ' · ' + cliente.telefone : '');
            item.append(nome, detalhe);
            item.addEventListener('mousedown', function (evento) {
                evento.preventDefault();
            });
            item.addEventListener('click', function () {
                opcoes.aoEscolher(cliente);
            });
            lista.appendChild(item);
        });
        lista.hidden = false;
        campo.setAttribute('aria-expanded', 'true');
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
        campo.setAttribute('aria-activedescendant', itens[destacada].id);
        itens[destacada].scrollIntoView({block: 'nearest'});
    }

    campo.addEventListener('keydown', function (evento) {
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
            opcoes.aoEscolher(sugestoes[destacada]);
        } else if (evento.key === 'Escape') {
            fechar();
        }
    });

    campo.addEventListener('blur', fechar);

    return {mostrar: mostrar, fechar: fechar};
}

// Busca as sugestões quando o texto do campo para de mudar por um instante; termos curtos fecham a lista.
// Só a resposta da última busca é mostrada, e só se o campo ainda estiver em foco.
function sugerirAoDigitar(campo, url, listaSugestoes) {
    var espera = null;
    var ultimaBusca = 0;
    campo.addEventListener('input', function () {
        clearTimeout(espera);
        var termo = campo.value.trim();
        if (termo.length < 2) {
            listaSugestoes.fechar();
            return;
        }
        espera = setTimeout(function () {
            var busca = ++ultimaBusca;
            buscarSugestoesCliente(url, termo)
                .then(function (clientes) {
                    if (busca === ultimaBusca && document.activeElement === campo) {
                        listaSugestoes.mostrar(clientes);
                    }
                })
                .catch(listaSugestoes.fechar);
        }, 250);
    });
}

// Campo de busca das listas com data-sugerir-cliente: sugere clientes (inclusive inativos) enquanto se digita.
// data-ao-escolher="filtrar" pesquisa pelo cliente escolhido usando o CPF, que é único (sem CPF, vai o nome);
// data-ao-escolher="abrir" abre a ficha do cliente.
document.querySelectorAll('[data-sugerir-cliente]').forEach(function (campo) {
    var lista = document.getElementById(campo.getAttribute('aria-controls'));
    var urlFicha = campo.getAttribute('data-url-ficha');
    var acao = campo.getAttribute('data-ao-escolher');
    var listaSugestoes = criarListaSugestoesCliente(campo, lista, {
        mensagemVazia: 'Nenhum cliente encontrado.',
        aoEscolher: function (cliente) {
            listaSugestoes.fechar();
            if (acao === 'abrir') {
                iniciarCarregamento();
                window.location.href = urlFicha + cliente.id;
                return;
            }
            campo.value = cliente.cpf || cliente.nome;
            campo.form.requestSubmit();
        }
    });
    sugerirAoDigitar(campo, campo.getAttribute('data-url-sugestoes'), listaSugestoes);
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
    var clienteDoCpf = null;
    var listaSugestoes = criarListaSugestoesCliente(campoNome, lista, {
        mensagemVazia: 'Nenhum cliente cadastrado encontrado. Preencha o CPF para cadastrar.',
        aoEscolher: escolher
    });

    function escolher(cliente) {
        campoId.value = cliente.id;
        campoNome.value = cliente.nome;
        campoNome.readOnly = true;
        campoCpf.value = cliente.cpf || '';
        campoCpf.readOnly = Boolean(cliente.cpf);
        campoNome.classList.remove('is-invalid');
        campoCpf.classList.remove('is-invalid');
        if (linkFicha) {
            linkFicha.href = urlFicha + cliente.id;
        }
        avisoSemCpf.hidden = Boolean(cliente.cpf);
        avisoEscolhido.hidden = false;
        ajudaNovo.hidden = true;
        avisoCpf.hidden = true;
        listaSugestoes.fechar();
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

    sugerirAoDigitar(campoNome, urlSugestoes, listaSugestoes);

    // CPF digitado à mão que já é de um cliente: oferece usar o cadastro em vez de esperar o erro ao salvar.
    campoCpf.addEventListener('change', function () {
        avisoCpf.hidden = true;
        clienteDoCpf = null;
        var digitos = campoCpf.value.replace(/\D/g, '');
        if (campoId.value || digitos.length !== 11) {
            return;
        }
        buscarSugestoesCliente(urlSugestoes, digitos)
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

// Carregamento: ao abrir outra tela ou enviar um formulário, a barra do topo aparece na hora e,
// se a resposta demorar, uma caixa "Carregando…" cobre a tela e evita cliques repetidos.
function iniciarCarregamento() {
    document.body.classList.add('carregando');
}

function linkAbreOutraTela(link, evento) {
    if (evento.defaultPrevented || evento.button !== 0 || evento.ctrlKey || evento.metaKey || evento.shiftKey || evento.altKey) {
        return false;
    }
    if ((link.target && link.target !== '_self') || link.hasAttribute('download')
        || link.hasAttribute('data-sem-carregamento') || link.hasAttribute('data-bs-toggle')) {
        return false;
    }
    var href = link.getAttribute('href');
    if (!href || href.charAt(0) === '#' || link.origin !== window.location.origin) {
        return false;
    }
    return !(link.pathname === window.location.pathname && link.search === window.location.search && link.hash);
}

document.addEventListener('click', function (evento) {
    var link = evento.target.closest('a[href]');
    if (!link || !linkAbreOutraTela(link, evento)) {
        return;
    }
    iniciarCarregamento();
    var menu = link.closest('.offcanvas-md.show');
    if (menu) {
        bootstrap.Offcanvas.getOrCreateInstance(menu).hide();
    }
});

// Campos de filtro escondidos (ex.: "De"/"Até" fora do período personalizado) não vão na pesquisa.
function desativarCamposEscondidos(formulario) {
    formulario.querySelectorAll('[data-mostrar-quando][hidden] input, [data-mostrar-quando][hidden] select')
        .forEach(function (campo) {
            if (!campo.disabled) {
                campo.disabled = true;
                campo.setAttribute('data-desativado-no-envio', '');
            }
        });
}

// Botão que enviou o formulário: fica desativado com um indicador até a próxima tela abrir.
function marcarBotaoEnviando(botao) {
    if (!botao || !botao.classList.contains('btn') || botao.classList.contains('visually-hidden-focusable')) {
        return;
    }
    botao.setAttribute('data-conteudo-original', botao.innerHTML);
    var texto = botao.textContent.trim();
    var spinner = document.createElement('span');
    spinner.className = 'spinner-border spinner-border-sm me-1';
    spinner.setAttribute('aria-hidden', 'true');
    botao.replaceChildren(spinner, document.createTextNode(texto === 'Salvar' ? 'Salvando…' : texto));
    botao.disabled = true;
}

document.addEventListener('submit', function (evento) {
    var formulario = evento.target;
    if (evento.defaultPrevented || formulario.hasAttribute('data-cadastro-rapido') || formulario.target === '_blank') {
        return;
    }
    if (formulario.method === 'get') {
        desativarCamposEscondidos(formulario);
    }
    iniciarCarregamento();
    var botao = evento.submitter;
    setTimeout(function () {
        marcarBotaoEnviando(botao);
    }, 0);
});

// Ao voltar pelo navegador, a página pode vir do cache ainda no estado "carregando".
window.addEventListener('pageshow', function () {
    document.body.classList.remove('carregando');
    document.querySelectorAll('[data-conteudo-original]').forEach(function (botao) {
        botao.innerHTML = botao.getAttribute('data-conteudo-original');
        botao.removeAttribute('data-conteudo-original');
        botao.disabled = false;
    });
    document.querySelectorAll('[data-desativado-no-envio]').forEach(function (campo) {
        campo.disabled = false;
        campo.removeAttribute('data-desativado-no-envio');
    });
});

// Etiquetas dos filtros aplicados: cada parâmetro da pesquisa atual que corresponde a um campo do
// formulário vira "Rótulo: valor ×". Campos com data-sem-etiqueta (período, mês) ficam de fora,
// e o valor igual ao data-padrao do campo não conta como filtro.
document.querySelectorAll('[data-etiquetas-filtro]').forEach(function (caixa) {
    var formulario = caixa.closest('form');
    var parametros = new URLSearchParams(window.location.search);
    var etiquetas = [];

    parametros.forEach(function (valor, nome) {
        var campo = formulario.elements.namedItem(nome);
        if (!campo || !campo.id || campo.type === 'hidden' || campo.hasAttribute('data-sem-etiqueta')
            || valor.trim() === '' || valor === (campo.getAttribute('data-padrao') || '')) {
            return;
        }
        var rotulo = formulario.querySelector('label[for="' + campo.id + '"]');
        var texto = valor;
        if (campo.tagName === 'SELECT') {
            var opcao = Array.prototype.find.call(campo.options, function (o) {
                return o.value === valor;
            });
            texto = opcao ? opcao.textContent.trim() : valor;
        }
        var semFiltro = new URLSearchParams(window.location.search);
        semFiltro.delete(nome);
        semFiltro.delete('pagina');
        var consulta = semFiltro.toString();

        var etiqueta = document.createElement('span');
        etiqueta.className = 'etiqueta-filtro';
        var descricao = document.createElement('span');
        descricao.textContent = (rotulo ? rotulo.textContent.trim() + ': ' : '') + texto;
        var remover = document.createElement('a');
        remover.href = window.location.pathname + (consulta ? '?' + consulta : '');
        remover.title = 'Remover este filtro';
        remover.setAttribute('aria-label', 'Remover filtro ' + descricao.textContent);
        remover.innerHTML = '<i class="bi bi-x-lg"></i>';
        etiqueta.append(descricao, remover);
        etiquetas.push(etiqueta);
    });

    if (etiquetas.length > 0) {
        var titulo = document.createElement('span');
        titulo.className = 'text-body-secondary';
        titulo.textContent = 'Filtros aplicados:';
        caixa.replaceChildren.apply(caixa, [titulo].concat(etiquetas));
        caixa.hidden = false;
    }
});

function parseValorBr(texto) {
    if (!texto) {
        return 0;
    }
    var limpo = String(texto).trim().replace(/R\$/g, '').replace(/\s/g, '');
    if (limpo.indexOf(',') >= 0) {
        limpo = limpo.replace(/\./g, '').replace(',', '.');
    }
    var valor = parseFloat(limpo);
    return isNaN(valor) ? 0 : valor;
}

function formatarValorBr(valor) {
    return valor.toFixed(2).replace('.', ',');
}

// Honorário "Parcial": "já pago" e "falta receber" se completam a partir do total (data-total, data-pago e
// data-falta são ids dos campos). Ao mudar o total, o campo digitado por último é mantido e o outro é recalculado.
function sincronizarHonorarioParcial(evento) {
    var alvo = evento && evento.target;
    document.querySelectorAll('[data-honorario-parcial]').forEach(function (aviso) {
        var total = document.getElementById(aviso.getAttribute('data-total'));
        var pago = document.getElementById(aviso.getAttribute('data-pago'));
        var falta = document.getElementById(aviso.getAttribute('data-falta'));
        if (!total || !pago || !falta) {
            return;
        }
        if (alvo === pago || alvo === falta) {
            aviso.setAttribute('data-ultimo', alvo === pago ? 'pago' : 'falta');
        }
        var valorTotal = parseValorBr(total.value);
        if (alvo === pago || alvo === falta || alvo === total) {
            var ultimo = aviso.getAttribute('data-ultimo') || (pago.value.trim() ? 'pago' : 'falta');
            var origem = ultimo === 'pago' ? pago : falta;
            var destino = ultimo === 'pago' ? falta : pago;
            var resto = valorTotal - parseValorBr(origem.value);
            destino.value = valorTotal > 0 && origem.value.trim() && resto > 0 ? formatarValorBr(resto) : '';
        }

        var valorPago = parseValorBr(pago.value);
        var valorFalta = parseValorBr(falta.value);
        var mensagem = '';
        if (valorTotal <= 0 && (valorPago > 0 || valorFalta > 0)) {
            mensagem = 'Preencha também o valor total do honorário.';
        } else if (valorTotal > 0 && valorPago >= valorTotal) {
            mensagem = 'O valor pago cobre todo o honorário: escolha "Recebido".';
        } else if (valorTotal > 0 && valorFalta >= valorTotal) {
            mensagem = 'Falta o honorário inteiro: se o cliente ainda não pagou nada, escolha "A receber".';
        }
        aviso.textContent = mensagem;
    });
}

document.addEventListener('input', sincronizarHonorarioParcial);
document.addEventListener('DOMContentLoaded', function () {
    sincronizarHonorarioParcial(null);
});

function adicionarMeses(dataIso, meses) {
    var partes = dataIso.split('-');
    var data = new Date(Number(partes[0]), Number(partes[1]) - 1, Number(partes[2]));
    data.setMonth(data.getMonth() + meses);
    var ano = data.getFullYear();
    var mes = String(data.getMonth() + 1).padStart(2, '0');
    var dia = String(data.getDate()).padStart(2, '0');
    return ano + '-' + mes + '-' + dia;
}

function dividirParcelasIguais(total, quantidade) {
    var centavos = Math.round(total * 100);
    var base = Math.floor(centavos / quantidade);
    var valores = [];
    var soma = 0;
    for (var i = 1; i < quantidade; i++) {
        valores.push(base);
        soma += base;
    }
    valores.push(centavos - soma);
    return valores.map(function (c) { return c / 100; });
}

document.querySelectorAll('[data-dividir-parcelas]').forEach(function (formulario) {
    var campoTotal = formulario.querySelector('[data-parcela-total]');
    var campoQuantidade = formulario.querySelector('[data-parcela-quantidade]');
    var campoPrimeiro = formulario.querySelector('[data-parcela-primeiro]');
    var campoDesconto = formulario.querySelector('[data-parcela-desconto]');
    var corpo = formulario.querySelector('[data-parcela-linhas]');
    var aviso = formulario.querySelector('[data-parcela-aviso]');
    var celulaSoma = formulario.querySelector('[data-parcela-soma]');
    var celulaAlvo = formulario.querySelector('[data-parcela-alvo]');
    var original = parseValorBr(formulario.getAttribute('data-valor-original'));
    var jaRecebido = parseValorBr(formulario.getAttribute('data-ja-recebido'));

    function centavos(valor) {
        return Math.round(valor * 100);
    }

    function alvoEmCentavos() {
        return Math.max(0, centavos(parseValorBr(campoTotal.value)) - centavos(jaRecebido));
    }

    function camposValor() {
        return Array.prototype.slice.call(corpo.querySelectorAll('input[name$=".valor"]'));
    }

    function atualizarDesconto() {
        if (!campoDesconto) {
            return;
        }
        var negociado = parseValorBr(campoTotal.value);
        var desconto = Math.max(0, original - negociado);
        campoDesconto.value = formatarValorBr(desconto);
    }

    function atualizarSoma() {
        if (!celulaSoma) {
            return;
        }
        var soma = camposValor().reduce(function (acc, campo) {
            return acc + centavos(parseValorBr(campo.value));
        }, 0);
        var alvo = alvoEmCentavos();
        celulaSoma.textContent = formatarValorBr(soma / 100);
        celulaSoma.classList.toggle('text-danger', soma !== alvo);
        celulaSoma.classList.toggle('text-success', soma === alvo && alvo > 0);
        if (celulaAlvo) {
            celulaAlvo.textContent = formatarValorBr(alvo / 100);
        }
    }

    function mostrarAviso(mensagem) {
        if (!aviso) {
            return;
        }
        aviso.textContent = mensagem || '';
        aviso.classList.toggle('d-none', !mensagem);
    }

    function distribuir(avisar) {
        var campos = camposValor();
        if (!campos.length) {
            return;
        }
        var manuais = campos.filter(function (campo) { return campo.dataset.manual === 'true'; });
        var livres = campos.filter(function (campo) { return campo.dataset.manual !== 'true'; });
        var fixado = manuais.reduce(function (acc, campo) {
            return acc + centavos(parseValorBr(campo.value));
        }, 0);
        var restante = alvoEmCentavos() - fixado;
        var problema = null;
        if (!livres.length) {
            problema = 'Todas as parcelas têm valor digitado. Apague o valor das parcelas que devem ser calculadas '
                + 'ou use "Dividir igualmente".';
        } else if (restante <= 0) {
            problema = 'Os valores digitados (' + formatarValorBr(fixado / 100) + ') já cobrem o valor a parcelar ('
                + formatarValorBr(alvoEmCentavos() / 100) + '). Ajuste os valores ou reduza a quantidade de parcelas.';
        }
        if (problema) {
            if (avisar) {
                mostrarAviso(problema);
            }
            atualizarSoma();
            return;
        }
        dividirParcelasIguais(restante / 100, livres.length).forEach(function (valor, indice) {
            livres[indice].value = formatarValorBr(valor);
        });
        mostrarAviso(null);
        atualizarSoma();
    }

    function criarLinha(indice, vencimentoIso, valorTexto, manual) {
        var linha = document.createElement('tr');
        var numero = document.createElement('td');
        numero.textContent = String(indice + 1);
        var vencimento = document.createElement('td');
        var campoVencimento = document.createElement('input');
        campoVencimento.type = 'date';
        campoVencimento.className = 'form-control';
        campoVencimento.name = 'parcelas[' + indice + '].vencimento';
        campoVencimento.required = true;
        campoVencimento.value = vencimentoIso;
        vencimento.appendChild(campoVencimento);
        var valorCelula = document.createElement('td');
        var campoValor = document.createElement('input');
        campoValor.type = 'text';
        campoValor.inputMode = 'decimal';
        campoValor.className = 'form-control text-end';
        campoValor.name = 'parcelas[' + indice + '].valor';
        campoValor.required = true;
        campoValor.value = valorTexto;
        if (manual) {
            campoValor.dataset.manual = 'true';
        }
        valorCelula.appendChild(campoValor);
        linha.append(numero, vencimento, valorCelula);
        return linha;
    }

    function redesenhar(recalcularVencimentos) {
        var quantidade = parseInt(campoQuantidade.value, 10);
        var primeiro = campoPrimeiro.value;
        if (!quantidade || quantidade < 1 || quantidade > 60 || !primeiro) {
            return;
        }
        var existentes = Array.prototype.map.call(corpo.rows, function (linha) {
            var campoValor = linha.querySelector('input[name$=".valor"]');
            return {
                vencimento: linha.querySelector('input[type="date"]').value,
                valor: campoValor.value,
                manual: campoValor.dataset.manual === 'true'
            };
        });
        var linhas = [];
        for (var i = 0; i < quantidade; i++) {
            var anterior = existentes[i];
            var vencimento = anterior && !recalcularVencimentos && anterior.vencimento
                ? anterior.vencimento : adicionarMeses(primeiro, i);
            var manual = !!(anterior && anterior.manual);
            linhas.push(criarLinha(i, vencimento, manual ? anterior.valor : '', manual));
        }
        corpo.replaceChildren.apply(corpo, linhas);
        distribuir(false);
    }

    corpo.addEventListener('input', function (evento) {
        var campo = evento.target;
        if (!campo.name || !/\.valor$/.test(campo.name)) {
            return;
        }
        if (campo.value.trim()) {
            campo.dataset.manual = 'true';
        } else {
            delete campo.dataset.manual;
        }
        atualizarSoma();
    });

    campoTotal.addEventListener('input', function () {
        atualizarDesconto();
        distribuir(false);
    });
    campoQuantidade.addEventListener('input', function () { redesenhar(false); });
    campoPrimeiro.addEventListener('change', function () { redesenhar(true); });

    var botaoCalcular = formulario.querySelector('[data-calcular-parcelas]');
    if (botaoCalcular) {
        botaoCalcular.addEventListener('click', function () { distribuir(true); });
    }
    var botaoIgual = formulario.querySelector('[data-dividir-igualmente]');
    if (botaoIgual) {
        botaoIgual.addEventListener('click', function () {
            camposValor().forEach(function (campo) { delete campo.dataset.manual; });
            distribuir(true);
        });
    }

    atualizarDesconto();
    atualizarSoma();
});
