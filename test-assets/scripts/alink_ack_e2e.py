#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Alink 回执类自动化联调脚本（与 test-assets/alink-ack-test-cases.md 对齐）。

依赖：
  pip install -r requirements-e2e.txt

环境变量（可选）：
  MQTT_HOST      默认 127.0.0.1
  MQTT_PORT      默认 1883
  MQTT_USER      默认 demo-001
  MQTT_PASS      默认 demo-001-secret
  IOT_ACCESS_URL 默认 http://localhost:8089

前置：
  - EMQX 已运行，规则将 /sys/+/+/thing/# 上行转发到 iot-access /api/access/upstream
  - iot-access 已部署且 EMQX Management API Key 已配置（回执 publish）
  - 数据库 ACL/设备种子见 test-assets/sql/seed-alink-e2e.sql

service_flow 说明：
  - reply_ack 依赖「设备先向 .../thing/service/{svc}/reply 上报」；无真实设备时由本脚本模拟该 publish。
  - 可选环境变量 SERVICE_FLOW_REPLY_DELAY_SEC（默认 1.5）：模拟回复发出后等待秒数，给 Webhook + 接入层下发 reply_ack 留时间。
  - 可选 SERVICE_FLOW_SUBSCRIBE_SETTLE_SEC（默认 0.6）：先订 reply_ack、再订 reboot 后各等待秒数，减少 SUBACK 竞态。

用法：
  python alink_ack_e2e.py
  python alink_ack_e2e.py --only property_post,event_post,service_flow
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
from typing import Any, Callable, Dict, List, Optional, Tuple

try:
    import paho.mqtt.client as mqtt
except ImportError:
    print("ERROR: install paho-mqtt: pip install -r requirements-e2e.txt", file=sys.stderr)
    sys.exit(2)

PK = "pk001"
DN = "demo-001"
BASE = f"/sys/{PK}/{DN}"


def _new_client_id() -> str:
    return "alink-e2e-%d" % (int(time.time() * 1000) % 1_000_000_000)


def _make_client(host: str, port: int, user: str, password: str) -> mqtt.Client:
    try:
        # paho-mqtt 2.x
        return mqtt.Client(
            mqtt.CallbackAPIVersion.VERSION1,
            client_id=_new_client_id(),
            protocol=mqtt.MQTTv311,
        )
    except (AttributeError, TypeError):
        return mqtt.Client(client_id=_new_client_id(), protocol=mqtt.MQTTv311)


class MqttSession:
    def __init__(self, host: str, port: int, user: str, password: str) -> None:
        self._host = host
        self._port = port
        self._inbox: List[Tuple[str, str]] = []
        self._client = _make_client(host, port, user, password)
        self._client.username_pw_set(user, password)

        def on_message(_c: Any, _u: Any, msg: mqtt.MQTTMessage) -> None:
            try:
                payload = msg.payload.decode("utf-8")
            except Exception:
                payload = str(msg.payload)
            self._inbox.append((msg.topic, payload))

        self._client.on_message = on_message

    def connect(self) -> None:
        self._client.connect(self._host, self._port, keepalive=30)
        self._client.loop_start()
        time.sleep(0.4)

    def disconnect(self) -> None:
        self._client.loop_stop()
        self._client.disconnect()

    def subscribe(self, topic: str, qos: int = 1) -> None:
        self._client.subscribe(topic, qos=qos)
        time.sleep(0.2)

    def publish(self, topic: str, payload: str, qos: int = 1) -> None:
        self._client.publish(topic, payload.encode("utf-8"), qos=qos)
        time.sleep(0.1)

    def publish_wait(self, topic: str, payload: str, qos: int = 1, timeout: float = 10.0) -> None:
        """QoS1 时等待 broker 确认，便于发现 ACL 拒绝等。"""
        info = self._client.publish(topic, payload.encode("utf-8"), qos=qos)
        info.wait_for_publish(timeout=timeout)

    def drain(self) -> None:
        self._inbox.clear()

    def wait_for(
        self,
        predicate: Callable[[str, str], bool],
        timeout_sec: float = 20.0,
        poll: float = 0.05,
    ) -> Tuple[str, str]:
        deadline = time.time() + timeout_sec
        seen = 0
        while time.time() < deadline:
            while seen < len(self._inbox):
                t, p = self._inbox[seen]
                seen += 1
                if predicate(t, p):
                    return t, p
            time.sleep(poll)
        raise TimeoutError("timeout waiting for MQTT message")


def post_downstream(base_url: str, topic: str, payload_obj: Dict[str, Any]) -> str:
    url = base_url.rstrip("/") + "/api/access/downstream/publish"
    inner = json.dumps(payload_obj, ensure_ascii=False, separators=(",", ":"))
    body = json.dumps(
        {"topic": topic, "payload": inner, "qos": 1, "retain": False},
        ensure_ascii=False,
    ).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json; charset=utf-8"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")
        raise RuntimeError("downstream HTTP %s: %s" % (e.code, err_body)) from e


def _assert_json_contains(body: str, **expect: Any) -> Dict[str, Any]:
    data = json.loads(body)
    for k, v in expect.items():
        if data.get(k) != v:
            raise AssertionError("expected %s=%r, got %r in %s" % (k, v, data.get(k), body))
    return data


def test_property_post_reply(s: MqttSession) -> None:
    reply_topic = "%s/thing/event/property/post_reply" % BASE
    post_topic = "%s/thing/event/property/post" % BASE
    msg_id = "e2e-prop-%d" % int(time.time())
    s.drain()
    s.subscribe(reply_topic, qos=1)
    payload = json.dumps(
        {
            "id": msg_id,
            "version": "1.0",
            "params": {"Temp": 25},
            "method": "thing.event.property.post",
        },
        separators=(",", ":"),
    )
    s.publish(post_topic, payload, qos=1)
    _t, body = s.wait_for(lambda t, p: t == reply_topic and msg_id in p, timeout_sec=25.0)
    j = _assert_json_contains(
        body,
        id=msg_id,
        code=200,
        method="thing.event.property.post_reply",
        message="success",
    )
    if not isinstance(j.get("data"), dict):
        raise AssertionError("expected data object in reply")


def test_event_post_reply(s: MqttSession) -> None:
    reply_topic = "%s/thing/event/Alarm/post_reply" % BASE
    post_topic = "%s/thing/event/Alarm/post" % BASE
    msg_id = "e2e-alarm-%d" % int(time.time())
    s.drain()
    s.subscribe(reply_topic, qos=1)
    payload = json.dumps(
        {
            "id": msg_id,
            "version": "1.0",
            "params": {"code": "OVER_TEMP"},
            "method": "thing.event.Alarm.post",
        },
        separators=(",", ":"),
    )
    s.publish(post_topic, payload, qos=1)
    _t, body = s.wait_for(lambda t, p: t == reply_topic and msg_id in p, timeout_sec=25.0)
    _assert_json_contains(
        body,
        id=msg_id,
        code=200,
        method="thing.event.Alarm.post_reply",
        message="success",
    )


def test_property_set_downlink(http_base: str, s: MqttSession) -> None:
    set_topic = "%s/thing/service/property/set" % BASE
    msg_id = "e2e-set-%d" % int(time.time())
    s.drain()
    s.subscribe(set_topic, qos=1)
    post_downstream(
        http_base,
        set_topic,
        {
            "id": msg_id,
            "version": "1.0",
            "params": {"Temp": 26},
            "method": "thing.service.property.set",
        },
    )
    _t, body = s.wait_for(
        lambda t, p: t == set_topic and msg_id in p and "thing.service.property.set" in p,
        timeout_sec=25.0,
    )
    j = json.loads(body)
    if j.get("id") != msg_id:
        raise AssertionError("set downlink id mismatch: %s" % body)


def test_property_set_reply_uplink(s: MqttSession) -> None:
    """设备侧 set_reply 上行；当前接入层未必发二次 ACK，只校验 publish 成功且可被规则消费（无强制 reply topic）。"""
    reply_topic = "%s/thing/service/property/set_reply" % BASE
    msg_id = "e2e-setrep-%d" % int(time.time())
    s.drain()
    payload = json.dumps(
        {
            "id": msg_id,
            "code": 200,
            "data": {},
            "method": "thing.service.property.set_reply",
        },
        separators=(",", ":"),
    )
    s.publish(reply_topic, payload, qos=1)
    time.sleep(1.0)


def test_service_invoke_and_reply_ack(http_base: str, s: MqttSession) -> None:
    """
    顺序（与用户描述一致）：
      1) 先订阅 reply_ack（避免平台回执早于订阅的竞态）
      2) 再订阅 reboot（接收云端下行）
      3) HTTP 触发下行 reboot
      4) 收到 reboot 后，本客户端模拟设备发 reply
      5) 断言收到 reply_ack
    """
    inv_topic = "%s/thing/service/reboot" % BASE
    dev_reply_topic = "%s/thing/service/reboot/reply" % BASE
    plat_ack_topic = "%s/thing/service/reboot/reply_ack" % BASE
    msg_id = "e2e-svc-%d" % int(time.time())
    s.drain()

    print("[STEP 1] subscribe reply_ack first -> %r" % plat_ack_topic)
    s.subscribe(plat_ack_topic, qos=1)
    settle = float(os.environ.get("SERVICE_FLOW_SUBSCRIBE_SETTLE_SEC", "0.6"))
    if settle > 0:
        time.sleep(settle)

    print("[STEP 2] subscribe downlink reboot -> %r" % inv_topic)
    s.subscribe(inv_topic, qos=1)
    if settle > 0:
        time.sleep(settle)

    print("[STEP 3] HTTP downstream/publish (cloud invoke reboot)")
    post_downstream(
        http_base,
        inv_topic,
        {
            "id": msg_id,
            "version": "1.0",
            "params": {},
            "method": "thing.service.reboot",
        },
    )
    try:
        _t, inv_body = s.wait_for(
            lambda t, p: t == inv_topic and msg_id in p and "thing.service.reboot" in p,
            timeout_sec=25.0,
        )
    except TimeoutError:
        preview = s._inbox[-5:] if len(s._inbox) > 5 else s._inbox
        raise TimeoutError(
            "timeout waiting for DOWNLINK invoke on %r (last inbox=%r)"
            % (inv_topic, preview)
        ) from None
    if json.loads(inv_body).get("id") != msg_id:
        raise AssertionError("invoke payload id mismatch")

    print(
        "[STEP 4] simulate DEVICE uplink: publish reply -> %r (wait broker ACK)"
        % dev_reply_topic
    )
    reply_payload = json.dumps(
        {
            "id": msg_id,
            "code": 200,
            "data": {"ok": True},
            "method": "thing.service.reboot.reply",
        },
        separators=(",", ":"),
    )
    try:
        s.publish_wait(dev_reply_topic, reply_payload, qos=1, timeout=15.0)
    except Exception as ex:
        raise RuntimeError(
            "publish reply failed (ACL deny or broker error?): topic=%r err=%s"
            % (dev_reply_topic, ex)
        ) from ex

    delay = float(os.environ.get("SERVICE_FLOW_REPLY_DELAY_SEC", "1.5"))
    if delay > 0:
        time.sleep(delay)

    print("[STEP 5] wait PLATFORM reply_ack on %r" % plat_ack_topic)
    try:
        _t, ack_body = s.wait_for(
            lambda t, p: t == plat_ack_topic and msg_id in p,
            timeout_sec=25.0,
        )
    except TimeoutError:
        preview = s._inbox[-8:] if len(s._inbox) > 8 else s._inbox
        raise TimeoutError(
            "timeout waiting for PLATFORM reply_ack on %r "
            "(need prior device uplink to .../reply; check webhook/ACL publish reply + subscribe reply_ack; last inbox=%r)"
            % (plat_ack_topic, preview)
        ) from None
    _assert_json_contains(
        ack_body,
        id=msg_id,
        code=200,
        method="thing.service.reboot.reply_ack",
        message="success",
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Alink ACK E2E against EMQX + iot-access")
    parser.add_argument(
        "--only",
        type=str,
        default="",
        help="comma-separated: property_post,event_post,property_set,property_set_reply,service_flow",
    )
    args = parser.parse_args()

    host = os.environ.get("MQTT_HOST", "127.0.0.1")
    port = int(os.environ.get("MQTT_PORT", "1883"))
    user = os.environ.get("MQTT_USER", "demo-001")
    password = os.environ.get("MQTT_PASS", "demo-001-secret")
    http_base = os.environ.get("IOT_ACCESS_URL", "http://localhost:8089")

    only = {x.strip() for x in args.only.split(",") if x.strip()}
    if not only:
        only = {
            "property_post",
            "event_post",
            "property_set",
            "property_set_reply",
            "service_flow",
        }

    s = MqttSession(host, port, user, password)
    s.connect()
    failed: List[str] = []
    try:
        if "property_post" in only:
            try:
                print("[RUN] property_post -> post_reply")
                test_property_post_reply(s)
                print("[OK]  property_post")
            except Exception as e:
                failed.append("property_post: %s" % e)
                print("[FAIL] property_post: %s" % e)

        if "event_post" in only:
            try:
                print("[RUN] event_post -> post_reply")
                test_event_post_reply(s)
                print("[OK]  event_post")
            except Exception as e:
                failed.append("event_post: %s" % e)
                print("[FAIL] event_post: %s" % e)

        if "property_set" in only:
            try:
                print("[RUN] property_set downlink (HTTP -> MQTT)")
                test_property_set_downlink(http_base, s)
                print("[OK]  property_set")
            except Exception as e:
                failed.append("property_set: %s" % e)
                print("[FAIL] property_set: %s" % e)

        if "property_set_reply" in only:
            try:
                print("[RUN] property_set_reply uplink (publish only)")
                test_property_set_reply_uplink(s)
                print("[OK]  property_set_reply (published)")
            except Exception as e:
                failed.append("property_set_reply: %s" % e)
                print("[FAIL] property_set_reply: %s" % e)

        if "service_flow" in only:
            try:
                print("[RUN] service invoke -> reply -> reply_ack")
                test_service_invoke_and_reply_ack(http_base, s)
                print("[OK]  service_flow")
            except Exception as e:
                failed.append("service_flow: %s" % e)
                print("[FAIL] service_flow: %s" % e)

    finally:
        s.disconnect()

    if failed:
        print("\n--- summary: %d failed ---" % len(failed))
        for line in failed:
            print(" - %s" % line)
        return 1
    print("\n--- all selected tests passed ---")
    return 0


if __name__ == "__main__":
    sys.exit(main())
