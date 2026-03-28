package com.casemanagement.dto.response;

import com.casemanagement.enums.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseResponse {
    private String id;
    private Integer srNo;
    private String apac;
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
    private UserResponse assignedUser;
    private List<CommentResponse> comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
