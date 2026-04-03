package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.mgmt.model.dto.CreateDeviceRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceImportErrorItem;
import cn.skylark.iot.mgmt.model.dto.DeviceImportResult;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class DeviceExcelImportService {

    private static final int MAX_DATA_ROWS = 2000;
    private static final String H_PRODUCT = "productkey";
    private static final String H_NAME = "devicename";
    private static final String H_ADDRESS = "address";

    private final DeviceService deviceService;

    public DeviceExcelImportService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public byte[] buildTemplateXlsx() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("devices");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("productKey");
            h.createCell(1).setCellValue("deviceName");
            h.createCell(2).setCellValue("address");
            Row ex = sheet.createRow(1);
            ex.createCell(0).setCellValue("replace-with-your-productKey");
            ex.createCell(1).setCellValue("Example device name");
            ex.createCell(2).setCellValue("");
            for (int i = 0; i < 3; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    public DeviceImportResult importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "file required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "only .xlsx supported");
        }
        DeviceImportResult result = new DeviceImportResult();
        try (InputStream in = file.getInputStream();
             XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new MgmtException(HttpStatus.BAD_REQUEST, "empty workbook");
            }
            int last = sheet.getLastRowNum();
            if (last > MAX_DATA_ROWS) {
                throw new MgmtException(HttpStatus.BAD_REQUEST,
                        "too many rows (max " + MAX_DATA_ROWS + " data rows)");
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new MgmtException(HttpStatus.BAD_REQUEST, "missing header row");
            }
            Map<String, Integer> col = parseHeader(headerRow);
            Integer iProduct = col.get(H_PRODUCT);
            Integer iName = col.get(H_NAME);
            if (iProduct == null || iName == null) {
                throw new MgmtException(HttpStatus.BAD_REQUEST,
                        "header must contain columns: productKey, deviceName");
            }
            Integer iAddr = col.get(H_ADDRESS);
            DataFormatter fmt = new DataFormatter();
            for (int r = 1; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String pk = cellValue(row, iProduct, fmt);
                String dn = cellValue(row, iName, fmt);
                String addr = iAddr != null ? cellValue(row, iAddr, fmt) : "";
                if (pk.isEmpty() && dn.isEmpty() && addr.isEmpty()) {
                    continue;
                }
                int excelRow = r + 1;
                if (pk.isEmpty()) {
                    addFail(result, excelRow, "productKey required");
                    continue;
                }
                if (dn.isEmpty()) {
                    addFail(result, excelRow, "deviceName required");
                    continue;
                }
                CreateDeviceRequest req = new CreateDeviceRequest();
                req.setDeviceName(dn);
                if (!addr.isEmpty()) {
                    req.setAddress(addr);
                }
                try {
                    deviceService.create(pk, req);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (MgmtException e) {
                    addFail(result, excelRow, e.getMessage());
                } catch (RuntimeException e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "unexpected error";
                    addFail(result, excelRow, msg);
                }
            }
        } catch (MgmtException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new MgmtException(HttpStatus.BAD_REQUEST, "failed to read excel: " + msg);
        }
        return result;
    }

    private static void addFail(DeviceImportResult result, int excelRow, String message) {
        result.getErrors().add(new DeviceImportErrorItem(excelRow, message));
        result.setFailCount(result.getFailCount() + 1);
    }

    private static Map<String, Integer> parseHeader(Row headerRow) {
        Map<String, Integer> m = new HashMap<>();
        DataFormatter fmt = new DataFormatter();
        int lastCell = headerRow.getLastCellNum();
        for (int c = 0; c < lastCell; c++) {
            String v = cellValue(headerRow, c, fmt).replace("\uFEFF", "").toLowerCase(Locale.ROOT).trim();
            if (!v.isEmpty() && !m.containsKey(v)) {
                m.put(v, c);
            }
        }
        return m;
    }

    private static String cellValue(Row row, int colIndex, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        if (row.getCell(colIndex) == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(colIndex)).trim();
    }
}
