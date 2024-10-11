package me.zhc1.pointsystem.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.zhc1.pointsystem.entity.PointBlock;
import me.zhc1.pointsystem.entity.User;

public interface PointBlockRepository extends JpaRepository<PointBlock, Integer> {
    List<PointBlock> findByUserAndExpiresAtGreaterThanOrderByExpiresAtAsc(User user, LocalDateTime now);

    @Query("SELECT SUM(pb.remainingAmount) FROM PointBlock pb WHERE pb.user = :user AND pb.expiresAt > :now")
    long sumRemainingAmountForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    List<PointBlock> findByExpiresAtLessThanEqual(LocalDateTime now);
}
