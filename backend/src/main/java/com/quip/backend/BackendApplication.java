package com.quip.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan({
		"com.quip.backend.member.mapper.database",
		"com.quip.backend.problem.mapper.database",
		"com.quip.backend.file.mapper.database",
		"com.quip.backend.server.mapper.database",
		"com.quip.backend.channel.mapper.database",
		"com.quip.backend.authorization.mapper.database",
})
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}