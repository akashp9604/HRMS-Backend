package com.configserver.hrm.leaveService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "leave_balance")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue
    @Column(length = 36)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeaveType leaveType;

    private int totalLeaves;
    private int usedLeaves;
    private int remainingLeaves;

    // NEW: Monthly paid leave tracking
    private int paidLeavesUsedThisMonth;
    private int month;
    private int year;
}