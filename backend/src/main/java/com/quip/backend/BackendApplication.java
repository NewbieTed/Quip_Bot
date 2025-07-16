package com.quip.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import com.quip.backend.config.SpringConfigurator;

@SpringBootApplication
@MapperScan({
		"com.quip.backend.member.mapper.database",
		"com.quip.backend.problem.mapper.database",
		"com.quip.backend.file.mapper.database",
		"com.quip.backend.server.mapper.database",
		"com.quip.backend.channel.mapper.database",
		"com.quip.backend.authorization.mapper.database",
})
public class BackendApplication implements ApplicationContextAware {

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		SpringConfigurator.setApplicationContext(applicationContext);
	}

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}