package me.zhc1.pointsystem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.zhc1.pointsystem.entity.PointBlock;
import me.zhc1.pointsystem.entity.PointUsage;
import me.zhc1.pointsystem.entity.RemainingPoint;
import me.zhc1.pointsystem.entity.User;
import me.zhc1.pointsystem.repository.PointBlockRepository;
import me.zhc1.pointsystem.repository.PointUsageRepository;
import me.zhc1.pointsystem.repository.RemainingPointRepository;
import me.zhc1.pointsystem.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointBlockRepository pointBlockRepository;

    @Mock
    private PointUsageRepository pointUsageRepository;

    @Mock
    private RemainingPointRepository remainingPointRepository;

    @InjectMocks
    private PointService pointService;

    private User testUser;
    private PointBlock expiredBlock;
    private PointBlock nonExpiredBlock;
    private RemainingPoint testRemainingPoint;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);

        LocalDateTime now = LocalDateTime.now();

        expiredBlock = new PointBlock();
        expiredBlock.setUser(testUser);
        expiredBlock.setRemainingAmount(100L);
        expiredBlock.setExpiresAt(now.minusDays(1));

        nonExpiredBlock = new PointBlock();
        nonExpiredBlock.setUser(testUser);
        nonExpiredBlock.setRemainingAmount(50L);
        nonExpiredBlock.setExpiresAt(now.plusDays(1));

        testRemainingPoint = new RemainingPoint();
        testRemainingPoint.setUserId(testUser.getUserId());
        testRemainingPoint.setTotalRemainingPoints(
                expiredBlock.getRemainingAmount() + nonExpiredBlock.getRemainingAmount());
        testRemainingPoint.setLastUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAvailablePoints_ShouldReturnCorrectAmount() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(remainingPointRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testRemainingPoint));

        Long availablePoints = pointService.getAvailablePoints(testUser.getUserId());

        assertEquals(testRemainingPoint.getTotalRemainingPoints(), availablePoints);
        verify(userRepository).findById(testUser.getUserId());
        verify(remainingPointRepository).findByUserId(testUser.getUserId());
    }

    @Test
    void getAvailablePoints_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> pointService.getAvailablePoints(testUser.getUserId()));
        verify(userRepository).findById(testUser.getUserId());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -100})
    void earnPoints_ShouldThrowException_WhenAmountIsZero(int amount) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.earnPoints(testUser.getUserId(), amount, LocalDateTime.now()));
        assertEquals("Amount must be positive", exception.getMessage());

        verify(userRepository, never()).findById(anyInt());
        verify(pointBlockRepository, never()).save(any());
    }

    @Test
    void earnPoints_ShouldCreateNewPointBlock() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        pointService.earnPoints(testUser.getUserId(), 100L, expiresAt);

        verify(userRepository).findById(testUser.getUserId());
        verify(pointBlockRepository)
                .save(argThat(pointBlock -> pointBlock.getUser().equals(testUser)
                        && pointBlock.getAmount() == 100L
                        && pointBlock.getRemainingAmount() == 100L
                        && pointBlock.getExpiresAt().equals(expiresAt)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -100})
    void usePoints_ShouldThrowException_WhenAmountIsZero(int amount) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> pointService.usePoints(testUser.getUserId(), amount));
        assertEquals("Amount must be positive", exception.getMessage());

        verify(userRepository, never()).findById(anyInt());
        verify(pointBlockRepository, never()).findByUserAndExpiresAtGreaterThanOrderByExpiresAtAsc(any(), any());
        verify(pointUsageRepository, never()).save(any());
    }

    @Test
    void usePoints_ShouldUsePointsCorrectly() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(pointBlockRepository.findByUserAndExpiresAtGreaterThanOrderByExpiresAtAsc(
                        eq(testUser), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(nonExpiredBlock));
        when(remainingPointRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testRemainingPoint));

        pointService.usePoints(testUser.getUserId(), 50L);

        verify(userRepository).findById(testUser.getUserId());
        verify(pointBlockRepository)
                .findByUserAndExpiresAtGreaterThanOrderByExpiresAtAsc(eq(testUser), any(LocalDateTime.class));
        verify(pointBlockRepository).save(argThat(pointBlock -> pointBlock.getRemainingAmount() == 0L));
        verify(pointUsageRepository)
                .save(argThat(pointUsage -> pointUsage.getUser().equals(testUser)
                        && pointUsage.getAmount() == 50L
                        && pointUsage.getUsageDetails().size() == 1
                        && pointUsage.getUsageDetails().getFirst().getAmount() == 50L));
    }

    @Test
    void usePoints_ShouldThrowException_WhenNotEnoughPoints() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(pointBlockRepository.findByUserAndExpiresAtGreaterThanOrderByExpiresAtAsc(
                        eq(testUser), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(nonExpiredBlock));

        assertThrows(IllegalStateException.class, () -> pointService.usePoints(testUser.getUserId(), 150L));

        verify(userRepository).findById(testUser.getUserId());
        verify(pointBlockRepository)
                .findByUserAndExpiresAtGreaterThanOrderByExpiresAtAsc(eq(testUser), any(LocalDateTime.class));
        verify(pointBlockRepository, never()).save(any(PointBlock.class));
        verify(pointUsageRepository, never()).save(any(PointUsage.class));
    }

    @Test
    void expirePoints_ShouldExpireCorrectPointsAndUpdateRemainingPoints() {
        when(pointBlockRepository.findByExpiresAtLessThanEqual(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(expiredBlock));
        when(remainingPointRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testRemainingPoint));

        pointService.expirePoints();

        verify(pointBlockRepository).save(argThat(block -> block.getRemainingAmount() == 0L));
        verify(remainingPointRepository).save(argThat(rp -> rp.getTotalRemainingPoints() == 50L));
    }
}
