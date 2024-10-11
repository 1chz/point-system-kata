package me.zhc1.pointsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import me.zhc1.pointsystem.entity.RemainingPoint;

public interface RemainingPointRepository extends JpaRepository<RemainingPoint, Integer> {
    Optional<RemainingPoint> findByUserId(int userId);
}
