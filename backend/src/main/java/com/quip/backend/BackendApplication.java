package com.quip.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import com.quip.backend.config.SpringConfigurator;
import com.quip.backend.config.redis.RedisProperties;
import com.quip.backend.config.redis.RedisCacheProperties;
import com.quip.backend.config.redis.AppRedisProperties;
import org.springframework.lang.NonNull;

/**
 * Main application class for the Quip backend service.
 * <p>
 * This class serves as the entry point for the Spring Boot application and configures
 * essential components like MyBatis mappers and application context.
 * </p>
 */
@SpringBootApplication
@EnableConfigurationProperties({
		RedisProperties.class,
		RedisCacheProperties.class,
		AppRedisProperties.class
})
// Configure MyBatis to scan for mapper interfaces in these packages
@MapperScan({
		"com.quip.backend.member.mapper.database",
		"com.quip.backend.problem.mapper.database",
		"com.quip.backend.file.mapper.database",
		"com.quip.backend.server.mapper.database",
		"com.quip.backend.channel.mapper.database",
		"com.quip.backend.authorization.mapper.database",
		"com.quip.backend.tool.mapper.database"
})
public class BackendApplication implements ApplicationContextAware {

	/**
	 * Sets the application context in the SpringConfigurator for global access.
	 * This allows components to access Spring beans from non-Spring managed classes.
	 */
	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
		SpringConfigurator.setApplicationContext(applicationContext);
	}

	/**
	 * Main method that starts the Spring Boot application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}