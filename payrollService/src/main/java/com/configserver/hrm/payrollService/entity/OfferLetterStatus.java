package com.configserver.hrm.payrollService.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "offer_letter_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferLetterStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", unique = true)
    private UUID employeeId;

    @Column(name = "employee_email") // ✅ Store employee email
    private String employeeEmail;

    @Column(name = "employee_name") // ✅ Store employee name
    private String employeeName;

    @Column(name = "accepted")
    private boolean accepted;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PreUpdate
    public void onAccept() {
        if (this.accepted && this.acceptedAt == null) {
            this.acceptedAt = LocalDateTime.now();
        }
    }
}