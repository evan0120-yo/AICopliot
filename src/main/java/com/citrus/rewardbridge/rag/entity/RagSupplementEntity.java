package com.citrus.rewardbridge.rag.entity;

import com.citrus.rewardbridge.source.entity.SourceEntity;
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
@Table(name = "rb_rag_supplement")
@Getter
@Setter
@NoArgsConstructor
public class RagSupplementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rag_id")
    private Long ragId;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "rag_type", nullable = false, length = 100)
    private String ragType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(name = "overridable", nullable = false)
    private boolean overridable;

    @Column(name = "retrieval_mode", nullable = false, length = 50)
    private String retrievalMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", referencedColumnName = "source_id", insertable = false, updatable = false)
    private SourceEntity source;

    public RagSupplementEntity(
            Long sourceId,
            String ragType,
            String title,
            String content,
            Integer orderNo,
            boolean overridable,
            String retrievalMode
    ) {
        this.sourceId = sourceId;
        this.ragType = ragType;
        this.title = title;
        this.content = content;
        this.orderNo = orderNo;
        this.overridable = overridable;
        this.retrievalMode = retrievalMode;
    }
}
