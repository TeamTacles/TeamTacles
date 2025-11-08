package br.com.teamtacles;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TeamtaclesApplication {

	public static void main(String[] args) {
		SpringApplication.run(TeamtaclesApplication.class, args);
	}
}