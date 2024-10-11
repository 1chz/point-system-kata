package me.zhc1.pointsystem.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "remaining_points")
public class RemainingPoint {
    @Id
    @Column(name = "user_id")
    private int userId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "total_remaining_points")
    private long totalRemainingPoints;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
}
