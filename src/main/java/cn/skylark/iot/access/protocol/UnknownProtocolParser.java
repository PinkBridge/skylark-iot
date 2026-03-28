package cn.skylark.iot.access.protocol;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(1000)
public class UnknownProtocolParser implements ProtocolHandler {

    @Override
    public boolean supports(ProtocolContext ctx) {
        return true;
    }

    @Override
    public ParseResult parse(ProtocolContext ctx) {
        ParseResult result = new ParseResult();
        result.setProtocolType(ctx == null ? "UNKNOWN" : ctx.getProtocolType());
        result.setParseStatus(ParseResult.ParseStatus.UNSUPPORTED);
        result.setParseError("unsupported protocol");
        result.setPayloadValid(false);
        result.setEventType("UNKNOWN_PROTOCOL");
        result.setMessageType("unknown");
        return result;
    }

    @Override
    public void ack(ParseResult result, ProtocolContext ctx) {
    }
}

