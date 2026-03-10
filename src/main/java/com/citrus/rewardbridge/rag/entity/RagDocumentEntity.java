package com.citrus.rewardbridge.rag.entity;

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
@Table(name = "rb_rag_document")
@Getter
@Setter
@NoArgsConstructor
public class RagDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_key", nullable = false, unique = true, length = 100)
    private String documentKey;

    @Column(name = "group_code", nullable = false)
    private Integer groupCode;

    @Column(name = "type_code", nullable = false)
    private Integer typeCode;

    @Column(name = "document_category", nullable = false, length = 30)
    private String documentCategory;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    public RagDocumentEntity(
            String documentKey,
            Integer groupCode,
            Integer typeCode,
            String documentCategory,
            String title,
            String content
    ) {
        this.documentKey = documentKey;
        this.groupCode = groupCode;
        this.typeCode = typeCode;
        this.documentCategory = documentCategory;
        this.title = title;
        this.content = content;
    }

}
