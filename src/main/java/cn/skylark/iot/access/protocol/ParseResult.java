package cn.skylark.iot.access.protocol;

import lombok.Data;

@Data
public class ParseResult {

    public enum ParseStatus {
        OK,
        INVALID,
        UNSUPPORTED
    }

    private ParseStatus parseStatus = ParseStatus.UNSUPPORTED;
    private String parseError;
    private String protocolType;
    private String eventType;
    private String messageType;
    private String messageId;
    private String alinkMethod;
    private String eventName;
    private String serviceName;
    private Boolean payloadValid = false;
}

