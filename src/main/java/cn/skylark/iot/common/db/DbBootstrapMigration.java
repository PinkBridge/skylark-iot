package cn.skylark.iot.common.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class DbBootstrapMigration {
    private static final Logger log = LoggerFactory.getLogger(DbBootstrapMigration.class);

    private final DataSource dataSource;

    public DbBootstrapMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        try (Connection conn = dataSource.getConnection()) {
            ensureIotDeviceConnectStatusColumn(conn);
            ensureIotDeviceConnectLastTimeColumns(conn);
            ensureIotDeviceAddressColumn(conn);
            ensureDeviceConnectRecordTable(conn);
            ensureDeviceGroupTables(conn);
        } catch (Exception e) {
            log.warn("db bootstrap migration failed: {}", e.getMessage(), e);
        }
    }

    private static void ensureIotDeviceConnectStatusColumn(Connection conn) throws Exception {
        if (columnExists(conn, "iot_device", "connect_status")) {
            return;
        }
        log.info("adding column iot_device.connect_status");
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE iot_device ADD COLUMN connect_status VARCHAR(16) NOT NULL DEFAULT 'disconnected' AFTER status");
        }
        try (Statement st = conn.createStatement()) {
            // Index is optional; ignore failure if it already exists.
            st.execute("CREATE INDEX idx_iot_device_connect_status ON iot_device(connect_status)");
        } catch (Exception ignored) {
        }
    }

    private static void ensureIotDeviceAddressColumn(Connection conn) throws Exception {
        if (columnExists(conn, "iot_device", "address")) {
            return;
        }
        log.info("adding column iot_device.address");
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE iot_device ADD COLUMN address VARCHAR(512) NULL AFTER device_name");
        }
    }

    private static void ensureDeviceConnectRecordTable(Connection conn) throws Exception {
        if (tableExists(conn, "iot_device_connect_record")) {
            return;
        }
        log.info("creating table iot_device_connect_record");
        try (Statement st = conn.createStatement()) {
            st.execute(
                    "CREATE TABLE IF NOT EXISTS iot_device_connect_record (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT," +
                            "tenant_id BIGINT NOT NULL DEFAULT 1," +
                            "product_key VARCHAR(64) NOT NULL," +
                            "device_key VARCHAR(64) NOT NULL," +
                            "action VARCHAR(16) NOT NULL," +
                            "client_id VARCHAR(128)," +
                            "ip VARCHAR(64)," +
                            "user_agent VARCHAR(255)," +
                            "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "PRIMARY KEY (id)," +
                            "KEY idx_iot_connect_record_lookup (tenant_id, product_key, device_key, created_at)" +
                            ")"
            );
        }
    }

    private static void ensureIotDeviceConnectLastTimeColumns(Connection conn) throws Exception {
        boolean hasConnectedAt = columnExists(conn, "iot_device", "last_connected_at");
        boolean hasDisconnectedAt = columnExists(conn, "iot_device", "last_disconnected_at");
        if (hasConnectedAt && hasDisconnectedAt) {
            return;
        }
        log.info("adding columns iot_device.last_connected_at / last_disconnected_at");
        try (Statement st = conn.createStatement()) {
            if (!hasConnectedAt) {
                st.execute("ALTER TABLE iot_device ADD COLUMN last_connected_at TIMESTAMP NULL AFTER connect_status");
            }
            if (!hasDisconnectedAt) {
                st.execute("ALTER TABLE iot_device ADD COLUMN last_disconnected_at TIMESTAMP NULL AFTER last_connected_at");
            }
        }
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE INDEX idx_iot_device_last_connected_at ON iot_device(last_connected_at)");
        } catch (Exception ignored) {
        }
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE INDEX idx_iot_device_last_disconnected_at ON iot_device(last_disconnected_at)");
        } catch (Exception ignored) {
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws Exception {
        String sql = "SELECT COUNT(1) FROM information_schema.COLUMNS " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws Exception {
        String sql = "SELECT COUNT(1) FROM information_schema.TABLES " +
                "WHERE table_schema = DATABASE() AND table_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    private static void ensureDeviceGroupTables(Connection conn) throws Exception {
        if (!tableExists(conn, "iot_device_group")) {
            log.info("creating table iot_device_group");
            try (Statement st = conn.createStatement()) {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS iot_device_group (" +
                                "id BIGINT NOT NULL AUTO_INCREMENT," +
                                "tenant_id BIGINT NOT NULL DEFAULT 1," +
                                "group_key VARCHAR(64) NOT NULL," +
                                "name VARCHAR(128) NOT NULL," +
                                "description VARCHAR(512)," +
                                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                                "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                                "PRIMARY KEY (id)," +
                                "UNIQUE KEY uk_iot_device_group_tenant_gk (tenant_id, group_key)," +
                                "UNIQUE KEY uk_iot_device_group_tenant_name (tenant_id, name)" +
                                ")"
                );
            }
        }
        if (!tableExists(conn, "iot_device_group_rel")) {
            log.info("creating table iot_device_group_rel");
            try (Statement st = conn.createStatement()) {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS iot_device_group_rel (" +
                                "id BIGINT NOT NULL AUTO_INCREMENT," +
                                "tenant_id BIGINT NOT NULL DEFAULT 1," +
                                "group_key VARCHAR(64) NOT NULL," +
                                "product_key VARCHAR(64) NOT NULL," +
                                "device_key VARCHAR(64) NOT NULL," +
                                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                                "PRIMARY KEY (id)," +
                                "UNIQUE KEY uk_iot_device_group_rel (tenant_id, group_key, product_key, device_key)," +
                                "KEY idx_iot_device_group_rel_lookup (tenant_id, group_key, created_at)," +
                                "KEY idx_iot_device_group_rel_device (tenant_id, product_key, device_key)" +
                                ")"
                );
            }
        }
    }
}

