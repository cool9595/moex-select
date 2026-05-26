package com.moexdelta.moexselect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
	title = "MOEX Select API",
	version = "1.0",
	description = "Человекоориентированный слой подбора финансовых инструментов поверх MOEX ISS. "
		+ "Информация не является индивидуальной инвестиционной рекомендацией. "
		+ "Подбор основан на выбранных пользователем параметрах и открытых рыночных данных MOEX ISS."
))
public class MoexSelectBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoexSelectBackendApplication.class, args);
	}

}
