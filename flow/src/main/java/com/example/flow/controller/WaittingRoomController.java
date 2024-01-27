package com.example.flow.controller;

import com.example.flow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class WaittingRoomController {

    private final UserQueueService userQueueService;

    @GetMapping("/watting-room")
    Mono<Rendering> waitingRoomPage(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId,
            @RequestParam(name = "redirect_url") String redirectUrl,
            ServerWebExchange exchange
    ) {
        var key = "user-queue-%s-token".formatted(queue);
        var cookieValue =  exchange.getRequest().getCookies().getFirst(key);
        var token = (cookieValue == null) ? "" : cookieValue.getValue();

        // page redicet 설정
        // proceed 인지 검사 맞으면 redirect
        return userQueueService.isAllowedByToken(queue, userId, token)
                .filter(allowed -> allowed)
                .flatMap(allowed -> Mono.just(Rendering.redirectTo(redirectUrl).build()))
                .switchIfEmpty(
                        // proceed 아닐시
                        // 없으면 재등록, 있으면 error 반환
                        userQueueService.rankWaitRegisterWaitQueue(queue, userId)
                                // 있으면 rank 조회
                                .onErrorResume(ex -> userQueueService.getRank(queue, userId))
                                .map(rank -> Rendering.view("waiting-room.html")
                                        .modelAttribute("number", rank)
                                        .modelAttribute("userId", userId)
                                        .modelAttribute("queue", queue)
                                        .build()
                                )
                );



    }

}
