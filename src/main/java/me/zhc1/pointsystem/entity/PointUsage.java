package me.zhc1.pointsystem.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "point_usages")
public class PointUsage {
    @Id
    @Column(name = "usage_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer usageId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private long amount;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @OneToMany(mappedBy = "usage", cascade = CascadeType.ALL)
    private List<PointUsageDetail> usageDetails = new ArrayList<>();
}
