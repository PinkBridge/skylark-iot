package cn.skylark.iot.access.protocol;

public interface ProtocolHandler {

    boolean supports(ProtocolContext ctx);

    ParseResult parse(ProtocolContext ctx);

    void ack(ParseResult result, ProtocolContext ctx);
}

