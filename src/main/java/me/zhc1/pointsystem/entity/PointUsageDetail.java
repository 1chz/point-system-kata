package me.zhc1.pointsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Getter
@Setter
@NoArgsConstructor
@Table(name = "point_usage_details")
public class PointUsageDetail {
    @Id
    @Column(name = "usage_detail_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer usageDetailId;

    @ManyToOne
    @JoinColumn(name = "usage_id", nullable = false)
    private PointUsage usage;

    @ManyToOne
    @JoinColumn(name = "block_id", nullable = false)
    private PointBlock block;

    @Column(nullable = false)
    private long amount;
}
