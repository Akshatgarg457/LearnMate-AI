package com.learnmate.backend.course.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.course.entity.*;
import com.learnmate.backend.course.repository.*;
import com.learnmate.backend.reward.entity.RewardTransaction;
import com.learnmate.backend.reward.service.RewardService;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepo;
    private final CourseRedemptionRepository redemptionRepo;
    private final UserRepository userRepo;
    private final RewardService rewardService;

    public CourseService(CourseRepository courseRepo, CourseRedemptionRepository redemptionRepo,
                         UserRepository userRepo, RewardService rewardService) {
        this.courseRepo = courseRepo;
        this.redemptionRepo = redemptionRepo;
        this.userRepo = userRepo;
        this.rewardService = rewardService;
    }

    public Page<Course> listCourses(String category, Course.CourseType type, Pageable pageable) {
        if (category != null) return courseRepo.findByActiveTrueAndCategory(category, pageable);
        if (type != null) return courseRepo.findByActiveTrueAndCourseType(type, pageable);
        return courseRepo.findByActiveTrue(pageable);
    }

    public Course getCourse(Long id) {
        return courseRepo.findById(id).filter(Course::isActive)
                .orElseThrow(() -> ApiException.notFound("Course not found"));
    }

    /**
     * Atomic redemption: validate → debit points → create entitlement.
     * Protected against duplicate redemption and concurrent over-spend.
     */
    @Transactional
    public CourseRedemption redeem(Long userId, Long courseId) {
        userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Course course = getCourse(courseId);

        if (redemptionRepo.existsByUserIdAndCourseIdAndStatus(userId, courseId, CourseRedemption.Status.ACTIVE)) {
            throw ApiException.conflict("You already have access to this course");
        }

        String idem = "course-redemption-" + userId + "-" + courseId;
        rewardService.debit(userId, course.getPointsPrice(),
                RewardTransaction.TransactionType.COURSE_REDEMPTION,
                "Redeemed course: " + course.getTitle(), idem, courseId);

        User user = userRepo.findById(userId).get();
        CourseRedemption redemption = new CourseRedemption();
        redemption.setUser(user);
        redemption.setCourse(course);
        redemption.setPointsSpent(course.getPointsPrice());
        redemption.setStatus(CourseRedemption.Status.ACTIVE);
        return redemptionRepo.save(redemption);
    }

    public Page<CourseRedemption> getMyCourses(Long userId, Pageable pageable) {
        return redemptionRepo.findByUserIdOrderByRedeemedAtDesc(userId, pageable);
    }

    public boolean hasAccess(Long userId, Long courseId) {
        return redemptionRepo.existsByUserIdAndCourseIdAndStatus(userId, courseId, CourseRedemption.Status.ACTIVE);
    }
}
