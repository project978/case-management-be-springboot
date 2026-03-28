package com.casemanagement.entity;

import com.casemanagement.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // sr_no from Excel
    private Integer srNo;

    // apac - loan account number
    @Column(name = "apac")
    private String apac;

    // party_name (customer name)
    @Column(name = "party_name")
    private String partyName;

    // customer_contact_no
    @Column(name = "customer_contact_no")
    private String customerContactNo;

    // fos - field officer name
    @Column(name = "fos")
    private String fos;

    // fos_contact_no
    @Column(name = "fos_contact_no")
    private String fosContactNo;

    // status (ho_status from Excel: CURED, FLOW, RB, STAB, ECS)
    @Enumerated(EnumType.STRING)
    @Column(name = "ho_status")
    private CaseStatus hoStatus;

    // bkt - bucket type (PTP, Cured, Roll back, Stab)
    @Column(name = "bkt")
    private String bkt;

    // emi amount
    @Column(name = "emi")
    private String emi;

    // pos - principal outstanding
    @Column(name = "pos", precision = 15, scale = 2)
    private BigDecimal pos;

    // case_value
    @Column(name = "case_value", precision = 15, scale = 2)
    private BigDecimal caseValue;

    // lm_fri
    @Column(name = "lm_fri", precision = 15, scale = 2)
    private BigDecimal lmFri;

    // cur_fri
    @Column(name = "cur_fri", precision = 15, scale = 2)
    private BigDecimal curFri;

    // od_without_fri
    @Column(name = "od_without_fri", precision = 15, scale = 2)
    private BigDecimal odWithoutFri;

    // od_with_fri
    @Column(name = "od_with_fri", precision = 15, scale = 2)
    private BigDecimal odWithFri;

    // m_collect_id
    @Column(name = "m_collect_id")
    private String mCollectId;

    // address
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    // asset_name
    @Column(name = "asset_name")
    private String assetName;

    // registration_number
    @Column(name = "registration_number")
    private String registrationNumber;

    // engine_no
    @Column(name = "engine_no")
    private String engineNo;

    // chassis_no
    @Column(name = "chassis_no")
    private String chassisNo;

    // Assigned user (for filter by user)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CaseComment> comments = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
