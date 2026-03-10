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
        name = "rb_source_rag_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_source_rag_mapping",
                columnNames = {"group_code", "type_code", "document_key"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class SourceRagMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", nullable = false)
    private Integer groupCode;

    @Column(name = "type_code", nullable = false)
    private Integer typeCode;

    @Column(name = "document_key", nullable = false, length = 100)
    private String documentKey;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public SourceRagMappingEntity(Integer groupCode, Integer typeCode, String documentKey, Integer sortOrder) {
        this.groupCode = groupCode;
        this.typeCode = typeCode;
        this.documentKey = documentKey;
        this.sortOrder = sortOrder;
    }
}
