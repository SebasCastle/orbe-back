package com.orbe;

import com.orbe.config.DotenvLoader;
import com.orbe.config.OrbeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OrbeProperties.class)
public class OrbeApplication {

	public static void main(String[] args) {
		DotenvLoader.load();
		SpringApplication.run(OrbeApplication.class, args);
	}
}
