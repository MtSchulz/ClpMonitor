package com.example.clpmonitor.model;

import java.time.LocalDateTime;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class TagLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDateTime operationTime;
    private String operationType; // "WRITE" ou "READ"
    private String plcIp;
    private String details;
    private boolean success;

    // Getters e Setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public LocalDateTime getOperationTime() {
        return operationTime;
    }
    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }
    public String getOperationType() {
        return operationType;
    }
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    public String getPlcIp() {
        return plcIp;
    }
    public void setPlcIp(String plcIp) {
        this.plcIp = plcIp;
    }
    public String getDetails() {
        return details;
    }
    public void setDetails(String details) {
        this.details = details;
    }
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
}
