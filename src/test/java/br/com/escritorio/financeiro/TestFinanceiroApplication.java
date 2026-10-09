package br.com.escritorio.financeiro;

import org.springframework.boot.SpringApplication;

public class TestFinanceiroApplication {

	public static void main(String[] args) {
		SpringApplication.from(FinanceiroApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
