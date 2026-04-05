package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.mgmt.model.dto.CreateDeviceRequest;
import cn.skylark.iot.mgmt.model.dto.CreateDeviceConnectRecordRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceConnectRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceCurrentPropertyResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceEventRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DevicePageQuery;
import cn.skylark.iot.mgmt.model.dto.DevicePageResponse;
import cn.skylark.iot.mgmt.model.dto.DevicePropertyRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DevicePropertyRecordResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceRecordPageQuery;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceServiceRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.ProductDataChannelResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateProductDataChannelRequest;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceRequest;

import java.util.List;

public interface DeviceService {

    DeviceResponse create(String productKey, CreateDeviceRequest req);

    DeviceResponse get(String productKey, String deviceKey);

    List<DeviceResponse> list(String productKey);

    List<DeviceResponse> listAll();

    DevicePageResponse listAllPage(DevicePageQuery query);

    DeviceResponse update(String productKey, String deviceKey, UpdateDeviceRequest req);

    DeviceResponse enable(String productKey, String deviceKey);

    DeviceResponse disable(String productKey, String deviceKey);

    DeviceResponse resetSecret(String productKey, String deviceKey);

    List<ProductDataChannelResponse> listDataChannels(String productKey, String deviceKey);

    void updateDataChannel(String productKey, String deviceKey, Long id, UpdateProductDataChannelRequest request);

    List<DeviceCurrentPropertyResponse> listCurrentProperties(String productKey, String deviceKey);

    DevicePropertyRecordResponse getLatestPropertyValue(String productKey, String deviceKey, String propertyIdentifier);

    DevicePropertyRecordPageResponse listPropertyRecords(String productKey, String deviceKey, DeviceRecordPageQuery query);

    DeviceEventRecordPageResponse listEventRecords(String productKey, String deviceKey, DeviceRecordPageQuery query);

    DeviceServiceRecordPageResponse listServiceRecords(String productKey, String deviceKey, DeviceRecordPageQuery query);

    DeviceConnectRecordPageResponse listConnectRecords(String productKey, String deviceKey, DeviceRecordPageQuery query);

    void createConnectRecord(String productKey, String deviceKey, CreateDeviceConnectRecordRequest request);

    void delete(String productKey, String deviceKey);
}
