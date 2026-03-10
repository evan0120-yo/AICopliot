package com.citrus.rewardbridge.source.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rb_source_reference_item")
@Getter
@Setter
@NoArgsConstructor
public class SourceReferenceItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", nullable = false)
    private Integer groupCode;

    @Column(name = "type_code", nullable = false)
    private Integer typeCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "reference_content", nullable = false, length = 500)
    private String referenceContent;

    @Column(name = "suggestion", nullable = false, length = 300)
    private String suggestion;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public SourceReferenceItemEntity(
            Integer groupCode,
            Integer typeCode,
            String itemName,
            String referenceContent,
            String suggestion,
            Integer sortOrder
    ) {
        this.groupCode = groupCode;
        this.typeCode = typeCode;
        this.itemName = itemName;
        this.referenceContent = referenceContent;
        this.suggestion = suggestion;
        this.sortOrder = sortOrder;
    }
}
