package com.integrateai.service;

import com.integrateai.dto.WebhookEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EventLogService {
    private static final Logger log = LoggerFactory.getLogger(EventLogService.class);
    private final List<WebhookEventDto> events = new CopyOnWriteArrayList<>();

    public void registerEvent(WebhookEventDto event) {
        log.info("[WEBHOOK_RECEIVED] Type: {}, ConnectionId: {}, Provider: {}",
                event.type(), event.connectionId(), event.providerConfigKey());
        events.add(0, event); // Latest first

        // Keep last 100 events
        if (events.size() > 100) {
            events.remove(events.size() - 1);
        }
    }

    public List<WebhookEventDto> getRecentEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public void clearEvents() {
        events.clear();
    }
}
