package com.devpulse.common.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RealtimeBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastServiceCheck(UUID orgId, Object payload) {
        messagingTemplate.convertAndSend("/topic/org." + orgId + ".status", payload);
    }

    public void broadcastIncidentUpdate(UUID orgId, Object payload) {
        messagingTemplate.convertAndSend("/topic/org." + orgId + ".incidents", payload);
    }
}

