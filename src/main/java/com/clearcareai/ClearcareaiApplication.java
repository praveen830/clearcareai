package com.clearcareai;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@EnableScheduling
@EnableCaching	
public class ClearcareaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClearcareaiApplication.class, args);
	}

}
