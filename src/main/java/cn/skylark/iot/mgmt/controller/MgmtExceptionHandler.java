package cn.skylark.iot.mgmt.controller;

import cn.skylark.iot.mgmt.service.MgmtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "cn.skylark.iot.mgmt.controller")
public class MgmtExceptionHandler {

    @ExceptionHandler(MgmtException.class)
    public ResponseEntity<Map<String, Object>> handleMgmtException(MgmtException ex) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("code", ex.getStatus().value());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("code", HttpStatus.BAD_REQUEST.value());
        String msg = ex.getBindingResult().getFieldError() == null
                ? "invalid request"
                : ex.getBindingResult().getFieldError().getDefaultMessage();
        body.put("message", msg);
        return ResponseEntity.badRequest().body(body);
    }
}

