package com.citrus.rewardbridge.source.entity;

import com.citrus.rewardbridge.builder.entity.SourceTemplateEntity;
import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
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
@Table(name = "rb_source")
@Getter
@Setter
@NoArgsConstructor
public class SourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "builder_id", nullable = false)
    private Integer builderId;

    @Column(name = "prompts", nullable = false, columnDefinition = "TEXT")
    private String prompts;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(name = "system_block", nullable = false)
    private boolean systemBlock;

    @Column(name = "needs_rag_supplement", nullable = false)
    private boolean needsRagSupplement;

    @Column(name = "copied_from_template_id")
    private Long copiedFromTemplateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "builder_id", referencedColumnName = "builder_id", insertable = false, updatable = false)
    private BuilderConfigEntity builderConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copied_from_template_id", referencedColumnName = "template_id", insertable = false, updatable = false)
    private SourceTemplateEntity copiedFromTemplate;

    public SourceEntity(Integer builderId, String prompts, Integer orderNo, boolean needsRagSupplement) {
        this(builderId, prompts, orderNo, false, needsRagSupplement, null);
    }

    public SourceEntity(
            Integer builderId,
            String prompts,
            Integer orderNo,
            boolean systemBlock,
            boolean needsRagSupplement
    ) {
        this(builderId, prompts, orderNo, systemBlock, needsRagSupplement, null);
    }

    public SourceEntity(
            Integer builderId,
            String prompts,
            Integer orderNo,
            boolean needsRagSupplement,
            Long copiedFromTemplateId
    ) {
        this(builderId, prompts, orderNo, false, needsRagSupplement, copiedFromTemplateId);
    }

    public SourceEntity(
            Integer builderId,
            String prompts,
            Integer orderNo,
            boolean systemBlock,
            boolean needsRagSupplement,
            Long copiedFromTemplateId
    ) {
        this.builderId = builderId;
        this.prompts = prompts;
        this.orderNo = orderNo;
        this.systemBlock = systemBlock;
        this.needsRagSupplement = needsRagSupplement;
        this.copiedFromTemplateId = copiedFromTemplateId;
    }
}
