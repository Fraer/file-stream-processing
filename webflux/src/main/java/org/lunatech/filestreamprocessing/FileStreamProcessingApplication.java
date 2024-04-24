package org.lunatech.filestreamprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication
public class FileStreamProcessingApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileStreamProcessingApplication.class, args);
	}

}
