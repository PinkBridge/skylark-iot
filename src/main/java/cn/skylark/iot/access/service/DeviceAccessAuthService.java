package cn.skylark.iot.access.service;

public interface DeviceAccessAuthService {

    boolean authenticate(String username, String password);

    boolean allowAcl(String username, String action, String topic);
}
