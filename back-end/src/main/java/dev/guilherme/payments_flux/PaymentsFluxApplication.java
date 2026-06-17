package dev.guilherme.payments_flux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class PaymentsFluxApplication {

	static void main(String[] args) {
		SpringApplication.run(PaymentsFluxApplication.class, args);
	}

}
