package cn.skylark.iot.access.model;

import lombok.Data;

@Data
public class InvokeThingServiceResponse {
    private Boolean success;
    private String requestId;
    private String errorMessage;
    private DataPayload data;

    @Data
    public static class DataPayload {
        private String messageId;
        private String result;
    }
}

