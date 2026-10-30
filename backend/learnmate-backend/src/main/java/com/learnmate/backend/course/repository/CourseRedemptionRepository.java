package com.learnmate.backend.course.repository;
import com.learnmate.backend.course.entity.CourseRedemption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseRedemptionRepository extends JpaRepository<CourseRedemption, Long> {
    Page<CourseRedemption> findByUserIdOrderByRedeemedAtDesc(Long userId, Pageable p);
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, CourseRedemption.Status status);
}
