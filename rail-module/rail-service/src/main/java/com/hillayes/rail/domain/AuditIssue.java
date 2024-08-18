package com.hillayes.rail.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Records the transactions that have been identified as issues by an audit report.
 */
@Entity(name = "audit_issue")
@Table(schema = "rails", name = "audit_issue")
@Getter
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class AuditIssue {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    /**
     * The user to which the identified report and transaction belongs.
     */
    @ToString.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The report to which that discovered this issue.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "report_config_id", nullable = false)
    private UUID reportConfigId;

    /**
     * The transaction that has been identified as an issue.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    /**
     * Echoes the booking date and time of the referenced transaction.
     */
    @ToString.Include
    @Column(name = "booking_datetime", nullable = false)
    private Instant bookingDateTime;

    /**
     * Indicates whether the issue has been acknowledged by the user.
     */
    @Setter
    @Column(name = "acknowledged", nullable = false)
    private boolean acknowledged;

    /**
     * A convenience factor method to create a new issue for the identified report and transaction.
     * @param reportConfig the identified report configuration.
     * @param transaction the identified transaction.
     * @return a new, un-persisted, issue for the identified report and transaction.
     */
    public static AuditIssue issueFor(AuditReportConfig reportConfig, AccountTransaction transaction) {
        return new Builder()
            .userId(reportConfig.getUserId())
            .reportConfigId(reportConfig.getId())
            .transactionId(transaction.getId())
            .bookingDateTime(transaction.getBookingDateTime())
            .acknowledged(false)
            .build();
    }
}
