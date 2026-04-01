package cn.skylark.iot.access.protocol;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class ProtocolResolver {

    private final ProtocolParserRegistry registry;

    public ProtocolResolver(ProtocolParserRegistry registry) {
        this.registry = registry;
    }

    public ProtocolHandleResult resolveAndHandle(ProtocolContext ctx) {
        ProtocolHandleResult handleResult = new ProtocolHandleResult();
        if (ctx == null) {
            ParseResult result = new ParseResult();
            result.setParseStatus(ParseResult.ParseStatus.INVALID);
            result.setParseError("protocol context is null");
            result.setPayloadValid(false);
            handleResult.setParseResult(result);
            return handleResult;
        }
        if (!StringUtils.hasText(ctx.getProtocolType())) {
            ctx.setProtocolType(detectProtocolType(ctx));
        } else {
            ctx.setProtocolType(ctx.getProtocolType().trim().toUpperCase(Locale.ROOT));
        }
        ProtocolHandler handler = registry.select(ctx);
        if (handler == null) {
            ParseResult result = new ParseResult();
            result.setProtocolType(ctx.getProtocolType());
            result.setParseStatus(ParseResult.ParseStatus.UNSUPPORTED);
            result.setParseError("no protocol parser matched");
            result.setPayloadValid(false);
            handleResult.setParseResult(result);
            return handleResult;
        }
        ParseResult result = handler.parse(ctx);
        if (result.getProtocolType() == null) {
            result.setProtocolType(ctx.getProtocolType());
        }
        handler.ack(result, ctx);
        handleResult.setParseResult(result);
        return handleResult;
    }

    private static String detectProtocolType(ProtocolContext ctx) {
        if (ctx == null || !StringUtils.hasText(ctx.getTopic())) {
            return "UNKNOWN";
        }
        String topic = ctx.getTopic().trim().toLowerCase(Locale.ROOT);
        if (topic.startsWith("/sys/")) {
            return "MQTT_ALINK_JSON";
        }
        return "UNKNOWN";
    }
}

