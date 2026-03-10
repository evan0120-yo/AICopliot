package com.citrus.rewardbridge.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rb_builder_config")
@Getter
@Setter
@NoArgsConstructor
public class BuilderConfigEntity {

    @Id
    @Column(name = "builder_id", nullable = false)
    private Integer builderId;

    @Column(name = "builder_code", nullable = false, unique = true, length = 100)
    private String builderCode;

    @Column(name = "group_label", nullable = false, length = 100)
    private String groupLabel;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "include_file", nullable = false)
    private boolean includeFile;

    @Column(name = "default_output_format", length = 30)
    private String defaultOutputFormat;

    @Column(name = "file_prefix", length = 120)
    private String filePrefix;

    @Column(name = "active", nullable = false)
    private boolean active;

    public BuilderConfigEntity(
            Integer builderId,
            String builderCode,
            String groupLabel,
            String name,
            String description,
            boolean includeFile,
            String defaultOutputFormat,
            String filePrefix,
            boolean active
    ) {
        this.builderId = builderId;
        this.builderCode = builderCode;
        this.groupLabel = groupLabel;
        this.name = name;
        this.description = description;
        this.includeFile = includeFile;
        this.defaultOutputFormat = defaultOutputFormat;
        this.filePrefix = filePrefix;
        this.active = active;
    }
}
