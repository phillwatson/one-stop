package com.hillayes.rail.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Audit report parameters are properties that are defined by audit report templates
 * and configured by the user. These properties are used to customize the factors on
 * which the report's algorithm is based.
 */
@Entity(name = "audit_report_parameter")
@Table(schema = "rails", name = "audit_report_parameter")
@Getter
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class AuditReportParameter {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    /**
     * The report to which this configuration parameter belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_config_id", nullable = false)
    private AuditReportConfig config;

    /**
     * The name of the configuration parameter.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "param_name", nullable = false)
    private String name;

    /**
     * The value of the configuration parameter.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Setter
    @Column(name = "param_value", nullable = false)
    private String value;
}
