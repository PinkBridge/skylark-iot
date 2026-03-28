package cn.skylark.iot.access.protocol;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProtocolParserRegistry {

    private final List<ProtocolHandler> handlers;

    public ProtocolParserRegistry(List<ProtocolHandler> handlers) {
        this.handlers = handlers;
    }

    public ProtocolHandler select(ProtocolContext ctx) {
        if (handlers == null || handlers.isEmpty()) {
            return null;
        }
        for (ProtocolHandler handler : handlers) {
            if (handler != null && handler.supports(ctx)) {
                return handler;
            }
        }
        return null;
    }
}

