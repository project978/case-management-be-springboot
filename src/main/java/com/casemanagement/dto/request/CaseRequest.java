package com.casemanagement.dto.request;

import com.casemanagement.enums.CaseStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CaseRequest {

    private Integer srNo;

    @NotBlank(message = "APAC is required")
    private String apac;

    @NotBlank(message = "Party name is required")
    private String partyName;

    private String customerContactNo;
    private String fos;
    private String fosContactNo;
    private CaseStatus hoStatus;
    private String bkt;
    private String emi;
    private BigDecimal pos;
    private BigDecimal caseValue;
    private BigDecimal lmFri;
    private BigDecimal curFri;
    private BigDecimal odWithoutFri;
    private BigDecimal odWithFri;
    private String mCollectId;
    private String address;
    private String assetName;
    private String registrationNumber;
    private String engineNo;
    private String chassisNo;
    private String assignedUserId;
}
