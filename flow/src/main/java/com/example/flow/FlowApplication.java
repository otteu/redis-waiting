package com.example.flow;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@RequiredArgsConstructor
public class FlowApplication{

//	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;


	public static void main(String[] args) {
		SpringApplication.run(FlowApplication.class, args);
	}

//	@Override
//	public void onApplicationEvent(ApplicationReadyEvent event) {
////		reactiveRedisTemplate.opsForValue().set("testkey", "testvalue").subscribe();
//	}
}
