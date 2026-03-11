package com.citrus.rewardbridge.builder.entity;

import com.citrus.rewardbridge.source.entity.SourceTypeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rb_source_template")
@Getter
@Setter
@NoArgsConstructor
public class SourceTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_key", nullable = false, unique = true, length = 100)
    private String templateKey;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "group_key", length = 100)
    private String groupKey;

    @Column(name = "type_code", nullable = false, length = 50)
    private String typeCode;

    @Column(name = "prompts", nullable = false, columnDefinition = "TEXT")
    private String prompts;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_code", referencedColumnName = "type_code", insertable = false, updatable = false)
    private SourceTypeEntity sourceType;

    public SourceTemplateEntity(
            String templateKey,
            String name,
            String description,
            String groupKey,
            String typeCode,
            String prompts,
            boolean active
    ) {
        this.templateKey = templateKey;
        this.name = name;
        this.description = description;
        this.groupKey = groupKey;
        this.typeCode = typeCode;
        this.prompts = prompts;
        this.active = active;
    }
}
