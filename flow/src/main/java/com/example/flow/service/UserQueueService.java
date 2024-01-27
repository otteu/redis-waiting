package com.example.flow.service;

import com.example.flow.dto.AllowUserResponse;
import com.example.flow.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.function.Tuples;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueueService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final String USER_QUEUE_WAIT_KEY = "users:queue:%s:wait";
    private final String USER_QUEUE_WAIT_KEY_FOR_SCAN = "users:queue:*:wait";
    private final String USER_QUEUE_PROCEED_KEY = "users:queue:%s:proceed";


    public Mono<Long> rankWaitRegisterWaitQueue(final String queue, final Long userId) {
        // 들오는 시간 체크
        var unixTimestamp = Instant.now().getEpochSecond();
        // key : userid, value : 시간
        return reactiveRedisTemplate.opsForZSet()
                .rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString())
                .map(i -> i >= 0 ? i + 1 : i)
                .switchIfEmpty(
                        reactiveRedisTemplate.opsForZSet()
                                .add(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString(), unixTimestamp)
                                .filter(i -> i)
                                .switchIfEmpty(Mono.error(ErrorCode.QUEUE_ALEADY_REGISTER_USER.build()))
                                .flatMap(i -> reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString()))
                                .map(i -> i >= 0 ? i + 1 : i)
                )
                ;

    }

    // 대기 번호 반환
    public Mono<Long> registerWaitQueue(final String queue, final Long userId) {

        // 들오는 시간 체크
        var unixTimestamp = Instant.now().getEpochSecond();
        // key : userid, value : 시간
        return reactiveRedisTemplate.opsForZSet()
                .add(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString(), unixTimestamp)
                // zset의 경우 key의 값이 동일 할시 false 반환
                .filter(i -> i)
                .switchIfEmpty(Mono.error(ErrorCode.QUEUE_ALEADY_REGISTER_USER.build()))
                .flatMap(i -> reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString()))
                .map(i -> i >= 0 ? i + 1 : i)
                ;

    }


    public Mono<Long> allowUser(final String queue, final Long count) {

        // pop 을 통해 queue의 갯수 뺀다.
        // wait queue 에 사용자 제거
        // proceed 에 추가
        return reactiveRedisTemplate.opsForZSet().popMin(USER_QUEUE_WAIT_KEY.formatted(queue), count)
                .flatMap(nember ->
                        reactiveRedisTemplate.opsForZSet()
                                .add(USER_QUEUE_PROCEED_KEY.formatted(queue), nember.getValue(), Instant.now().getEpochSecond()))
                .count()
                ;
    }

    // proceed 되었는지
    public Mono<Boolean> isAllowed(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_PROCEED_KEY.formatted(queue), userId.toString())
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0)
                ;
    }

    public Mono<Boolean> isAllowedByToken(final String queue, final Long userId, final String token) {
        return this.generateToken(queue, userId)
                .filter(gen -> gen.equalsIgnoreCase(token))
                .map(i -> true)
                .defaultIfEmpty(false);
    }

    public Mono<Long> getRank(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString())
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0 ? rank + 1 :rank)
                ;
    }

    public Mono<String> generateToken(final String queue, final Long userId) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            var input = "user-queue-%s-%d".formatted(queue, userId);
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuffer haxString = new StringBuffer();
            for(byte aByte: encodedHash) {
                haxString.append(String.format("%02x", aByte));
            }
            return Mono.just(haxString.toString());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }


    }

    @Scheduled(initialDelay = 5000, fixedDelay = 3000)
    public  void scheduleAllowedUser() {

        log.info("called scheduling...");

        var maxAllowUserCount = 3L;

        reactiveRedisTemplate.scan(ScanOptions.scanOptions()
                .match(USER_QUEUE_WAIT_KEY_FOR_SCAN)
                .count(100)
                .build())
                .map(key -> key.split(":")[2])
                .flatMap(queue -> allowUser(queue, maxAllowUserCount).map(allowed -> Tuples.of(queue, allowed)))
                .doOnNext(tuple -> log.info("Tried $d and allowed %d memebers of %s queue".formatted(maxAllowUserCount, tuple.getT2(), tuple.getT1())))
                .subscribe();
        ;

    }

}
