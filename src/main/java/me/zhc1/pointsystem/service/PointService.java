package me.zhc1.pointsystem.service;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import me.zhc1.pointsystem.entity.PointBlock;
import me.zhc1.pointsystem.entity.PointUsage;
import me.zhc1.pointsystem.entity.PointUsageDetail;
import me.zhc1.pointsystem.entity.RemainingPoint;
import me.zhc1.pointsystem.entity.User;
import me.zhc1.pointsystem.repository.PointBlockRepository;
import me.zhc1.pointsystem.repository.PointUsageRepository;
import me.zhc1.pointsystem.repository.RemainingPointRepository;
import me.zhc1.pointsystem.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {
    private final UserRepository userRepository;
    private final PointBlockRepository pointBlockRepository;
    private final PointUsageRepository pointUsageRepository;
    private final RemainingPointRepository remainingPointRepository;

    public long getAvailablePoints(int userId) {
        return userRepository
                .findById(userId)
                .map(user -> remainingPointRepository
                        .findByUserId(userId)
                        .map(RemainingPoint::getTotalRemainingPoints)
                        .orElse(0L))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional
    public void earnPoints(int userId, long amount, LocalDateTime expiresAt) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        PointBlock pointBlock = new PointBlock();
        pointBlock.setUser(user);
        pointBlock.setAmount(amount);
        pointBlock.setRemainingAmount(amount);
        pointBlock.setEarnedAt(LocalDateTime.now());
        pointBlock.setExpiresAt(expiresAt);

        pointBlockRepository.save(pointBlock);
        updateRemainingPoints(user, amount);
    }

    @Transactional
    public void usePoints(int userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        List<PointBlock> availablePointBlocks =
                pointBlockRepository.findByUserAndExpiresAtGreaterThanOrderByExpiresAtAsc(user, LocalDateTime.now());

        long totalAvailablePoints = availablePointBlocks.stream()
                .mapToLong(PointBlock::getRemainingAmount)
                .sum();
        if (totalAvailablePoints < amount) {
            throw new IllegalStateException("Not enough points available for user id: %s. Required: %s, Available: %s"
                    .formatted(userId, amount, totalAvailablePoints));
        }

        PointUsage pointUsage = new PointUsage();
        pointUsage.setUser(user);
        pointUsage.setAmount(amount);
        pointUsage.setUsedAt(LocalDateTime.now());

        long remainingToUse = amount;
        for (PointBlock pointBlock : availablePointBlocks) {
            if (remainingToUse <= 0) break;

            // Use the minimum of remaining points in the block and remaining points to use
            long usageAmount = Math.min(pointBlock.getRemainingAmount(), remainingToUse);
            pointBlock.setRemainingAmount(pointBlock.getRemainingAmount() - usageAmount);
            pointBlockRepository.save(pointBlock);

            // Append a usage detail record
            PointUsageDetail pointUsageDetail = new PointUsageDetail();
            pointUsageDetail.setUsage(pointUsage);
            pointUsageDetail.setBlock(pointBlock);
            pointUsageDetail.setAmount(usageAmount);
            pointUsage.getUsageDetails().add(pointUsageDetail);

            remainingToUse -= usageAmount;
        }

        pointUsageRepository.save(pointUsage);
        updateRemainingPoints(user, -amount);
    }

    // This is temporary code. In reality, it should be called every time the user redeems points.
    @Scheduled(cron = "0 0 0 * * ?") // Run every day at midnight
    public void expirePoints() {
        List<PointBlock> expiredBlocks = pointBlockRepository.findByExpiresAtLessThanEqual(LocalDateTime.now());

        for (PointBlock block : expiredBlocks) {
            if (block.getRemainingAmount() > 0) {
                // TODO: Log records or save history for expired points
                updateRemainingPoints(block.getUser(), -block.getRemainingAmount());
                block.setRemainingAmount(0L);
                pointBlockRepository.save(block);
            }
        }
    }

    private void updateRemainingPoints(User user, long amount) {
        RemainingPoint remainingPoint = remainingPointRepository
                .findByUserId(user.getUserId())
                .orElseGet(() -> {
                    RemainingPoint rp = new RemainingPoint();
                    rp.setUser(user);
                    return rp;
                });

        long newTotal = remainingPoint.getTotalRemainingPoints() + amount;
        if (newTotal < 0) {
            throw new IllegalStateException("Remaining points cannot be negative");
        }

        remainingPoint.setTotalRemainingPoints(newTotal);
        remainingPoint.setLastUpdatedAt(LocalDateTime.now());

        remainingPointRepository.save(remainingPoint);
    }
}
