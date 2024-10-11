package me.zhc1.pointsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import me.zhc1.pointsystem.entity.PointUsage;
import me.zhc1.pointsystem.entity.User;

public interface PointUsageRepository extends JpaRepository<PointUsage, Integer> {
    List<PointUsage> findByUserOrderByUsedAtDesc(User user);
}
