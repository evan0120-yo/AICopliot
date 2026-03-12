package com.citrus.rewardbridge.builder.entity;
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

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(name = "prompts", nullable = false, columnDefinition = "TEXT")
    private String prompts;

    @Column(name = "active", nullable = false)
    private boolean active;

    public SourceTemplateEntity(
            String templateKey,
            String name,
            String description,
            String groupKey,
            Integer orderNo,
            String prompts,
            boolean active
    ) {
        this.templateKey = templateKey;
        this.name = name;
        this.description = description;
        this.groupKey = groupKey;
        this.orderNo = orderNo;
        this.prompts = prompts;
        this.active = active;
    }
}
