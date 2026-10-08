package com.integrateai.controller;

import com.integrateai.dto.WebhookEventDto;
import com.integrateai.service.EventLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final EventLogService eventLogService;

    public WebhookController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @PostMapping("/nango")
    public ResponseEntity<Map<String, Object>> handleNangoWebhook(
            @RequestHeader(value = "X-Nango-Signature", required = false) String signature,
            @RequestBody WebhookEventDto event) {

        log.info("[HTTP_POST] /api/webhooks/nango signaturePresent={} type={}",
                signature != null, event.type());

        eventLogService.registerEvent(event);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Webhook received and logged successfully",
                "type", event.type() != null ? event.type() : "unknown"
        ));
    }

    @GetMapping("/events")
    public ResponseEntity<List<WebhookEventDto>> getWebhookEvents() {
        return ResponseEntity.ok(eventLogService.getRecentEvents());
    }

    @DeleteMapping("/events")
    public ResponseEntity<Map<String, String>> clearWebhookEvents() {
        eventLogService.clearEvents();
        return ResponseEntity.ok(Map.of("message", "Webhook events cleared"));
    }
}
