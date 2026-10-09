package br.com.escritorio.financeiro.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.admin")
public record AdminInicialProperties(String username, String password, String nome) {
}
