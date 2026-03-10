package com.citrus.rewardbridge.source.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rb_source_type")
@Getter
@Setter
@NoArgsConstructor
public class SourceTypeEntity {

    @Id
    @Column(name = "type_id", nullable = false)
    private Integer typeId;

    @Column(name = "type_code", nullable = false, unique = true, length = 50)
    private String typeCode;

    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "sort_priority", nullable = false)
    private Integer sortPriority;

    public SourceTypeEntity(Integer typeId, String typeCode, String typeName, String description, Integer sortPriority) {
        this.typeId = typeId;
        this.typeCode = typeCode;
        this.typeName = typeName;
        this.description = description;
        this.sortPriority = sortPriority;
    }
}
