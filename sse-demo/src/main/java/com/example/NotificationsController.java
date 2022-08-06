package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/notification")
@CrossOrigin("*")
public class NotificationsController {

    Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private Map<String, SseEmitter> emitters = new ConcurrentHashMap<String, SseEmitter>();

    @GetMapping("/subscribe")
    SseEmitter subscribe(@RequestParam("clientId") String clientId){

        if(emitters.get(clientId) != null) return emitters.get(clientId);

        SseEmitter emitter = new SseEmitter(180000l);
        emitter.onCompletion(() -> emitters.remove(clientId));
        emitter.onTimeout(() -> emitters.remove(clientId));
        emitter.onError(e -> {
            log.error("Create SseEmitter exception", e);
            emitters.remove(clientId);
        });
        emitters.put(clientId, emitter);
        log.info("{}", emitters.keySet());
        try {
            emitter.send(SseEmitter.event()
                                   .data("subscribed")
                                   .id(clientId));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @GetMapping("/send")
    void send(){
        for(String emitterId : emitters.keySet()){
            log.info("sending to {}", emitterId);
            SseEmitter sseEmitter = emitters.get(emitterId);
            try {
                sseEmitter.send("sse event - " + LocalDateTime.now());
            } catch (IOException e) {
                sseEmitter.completeWithError(e);
                emitters.remove(emitterId);
            }
        }
    }
}
