package com.citrus.rewardbridge.source.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "rb_source_scenario_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_source_scenario", columnNames = {"group_code", "type_code"})
)
@Getter
@Setter
@NoArgsConstructor
public class SourceScenarioConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", nullable = false)
    private Integer groupCode;

    @Column(name = "type_code", nullable = false)
    private Integer typeCode;

    @Column(name = "summary", nullable = false, length = 200)
    private String summary;

    public SourceScenarioConfigEntity(Integer groupCode, Integer typeCode, String summary) {
        this.groupCode = groupCode;
        this.typeCode = typeCode;
        this.summary = summary;
    }
}
