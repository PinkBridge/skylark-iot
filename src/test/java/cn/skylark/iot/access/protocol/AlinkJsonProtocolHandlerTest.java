package cn.skylark.iot.access.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.skylark.iot.access.model.DownstreamPublishRequest;
import cn.skylark.iot.access.service.EmqxManagementClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlinkJsonProtocolHandlerTest {

    private final List<DownstreamPublishRequest> published = new ArrayList<>();
    private final EmqxManagementClient emqx = req -> {
        published.add(req);
        return Optional.of("ok");
    };
    private final AlinkJsonProtocolHandler handler = new AlinkJsonProtocolHandler(new ObjectMapper(), emqx);

    @Test
    void shouldParsePropertyAndBuildAck() {
        published.clear();
        ProtocolContext ctx = new ProtocolContext();
        ctx.setTopic("/sys/pk001/demo-001/thing/event/property/post");
        ctx.setPayload("{\"id\":\"1001\",\"version\":\"1.0\",\"params\":{\"temp\":25},\"method\":\"thing.event.property.post\"}");

        ParseResult result = handler.parse(ctx);
        handler.ack(result, ctx);

        Assertions.assertEquals(ParseResult.ParseStatus.OK, result.getParseStatus());
        Assertions.assertEquals("PROPERTY_POST", result.getEventType());
        Assertions.assertEquals(1, published.size());
        Assertions.assertEquals("/sys/pk001/demo-001/thing/event/property/post_reply", published.get(0).getTopic());
        Assertions.assertTrue(published.get(0).getPayload().contains("\"id\":\"1001\""));
        Assertions.assertTrue(published.get(0).getPayload().contains("\"code\":200"));
    }

    @Test
    void shouldParseEventAndBuildAck() {
        published.clear();
        ProtocolContext ctx = new ProtocolContext();
        ctx.setTopic("/sys/pk001/demo-001/thing/event/alert/post");
        ctx.setPayload("{\"id\":\"1002\",\"version\":\"1.0\",\"params\":{\"level\":\"high\"},\"method\":\"thing.event.alert.post\"}");

        ParseResult result = handler.parse(ctx);
        handler.ack(result, ctx);

        Assertions.assertEquals(ParseResult.ParseStatus.OK, result.getParseStatus());
        Assertions.assertEquals("EVENT_POST", result.getEventType());
        Assertions.assertEquals("alert", result.getEventName());
        Assertions.assertEquals(1, published.size());
        Assertions.assertEquals("/sys/pk001/demo-001/thing/event/alert/post_reply", published.get(0).getTopic());
        Assertions.assertTrue(published.get(0).getPayload().contains("\"method\":\"thing.event.alert.post_reply\""));
    }

    @Test
    void shouldPreserveEventIdCasingInAckTopic() {
        published.clear();
        ProtocolContext ctx = new ProtocolContext();
        ctx.setTopic("/sys/pk001/demo-001/thing/event/Alarm/post");
        ctx.setPayload("{\"id\":\"2001\",\"version\":\"1.0\",\"params\":{\"code\":\"x\"},\"method\":\"thing.event.Alarm.post\"}");

        ParseResult result = handler.parse(ctx);
        handler.ack(result, ctx);

        Assertions.assertEquals("EVENT_POST", result.getEventType());
        Assertions.assertEquals("Alarm", result.getEventName());
        Assertions.assertEquals(1, published.size());
        Assertions.assertEquals("/sys/pk001/demo-001/thing/event/Alarm/post_reply", published.get(0).getTopic());
        Assertions.assertTrue(published.get(0).getPayload().contains("\"method\":\"thing.event.Alarm.post_reply\""));
    }

    @Test
    void shouldParseServiceReplyAndBuildAck() {
        published.clear();
        ProtocolContext ctx = new ProtocolContext();
        ctx.setTopic("/sys/pk001/demo-001/thing/service/reboot/reply");
        ctx.setPayload("{\"id\":\"1003\",\"code\":200,\"data\":{},\"method\":\"thing.service.reboot.reply\"}");

        ParseResult result = handler.parse(ctx);
        handler.ack(result, ctx);

        Assertions.assertEquals(ParseResult.ParseStatus.OK, result.getParseStatus());
        Assertions.assertEquals("SERVICE_REPLY", result.getEventType());
        Assertions.assertEquals("reboot", result.getServiceName());
        Assertions.assertEquals(1, published.size());
        Assertions.assertEquals("/sys/pk001/demo-001/thing/service/reboot/reply_ack", published.get(0).getTopic());
        Assertions.assertTrue(published.get(0).getPayload().contains("\"method\":\"thing.service.reboot.reply_ack\""));
    }
}

