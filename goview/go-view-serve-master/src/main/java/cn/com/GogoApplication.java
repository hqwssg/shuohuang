package cn.com;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("cn.com.v2.mapper")
@EnableAsync
public class GogoApplication {

	public static void main(String[] args) {
		SpringApplication.run(GogoApplication.class, args);
	}

}
