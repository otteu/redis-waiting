package com.example.website.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;

@Controller
@Slf4j
public class WebPointController {
    RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/")
    public String index(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId,
            HttpServletRequest request
    ) {
        var cookies = request.getCookies();
        var cookieName = "user-queue-%s-token".formatted(queue);

        String token = "";
        if(cookies != null) {
            var cookie = Arrays.stream(cookies).filter(i -> i.getName().equalsIgnoreCase(cookieName)).findFirst();
            token = cookie.orElse(new Cookie(cookieName, "")).getValue();
        }

        // ?user_id=10
        var uri = UriComponentsBuilder
                .fromUriString("http://127.0.0.1:9010")
                .path("/api/v1/queue/allowed")
                .queryParam("queue", queue)
                .queryParam("user_id", userId)
                .queryParam("token", token)
                .encode()
                .build()
                .toUri();
        log.info("sssdsdsdsddss");

        ResponseEntity<AllowedUserResponse> response = restTemplate.getForEntity(uri, AllowedUserResponse.class);
        if (response.getBody() == null || !response.getBody().allowed()) {
            return ("redirect:http://127.0.0.1:9010/watting-room?" +
                    "user_id=%d" +
                    "&redirect_url=%s").formatted(
                    userId, "http://127.0.0.1:9000?user_id=%d".formatted(userId)
            );
        }

        return "index";
    }

    public record AllowedUserResponse(Boolean allowed) {

    }
}
