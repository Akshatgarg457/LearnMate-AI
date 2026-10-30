package com.learnmate.backend.admin.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.community.entity.ContentReport;
import com.learnmate.backend.community.repository.ContentReportRepository;
import com.learnmate.backend.course.entity.Course;
import com.learnmate.backend.course.repository.CourseRepository;
import com.learnmate.backend.exam.entity.*;
import com.learnmate.backend.exam.repository.*;
import com.learnmate.backend.peer.entity.PeerSession;
import com.learnmate.backend.peer.repository.PeerSessionRepository;
import com.learnmate.backend.reward.entity.RewardTransaction;
import com.learnmate.backend.reward.service.RewardService;
import com.learnmate.backend.user.dto.UserResponse;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AdminService {

    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final QuizRepository quizRepo;
    private final MockExamRepository mockExamRepo;
    private final PeerSessionRepository peerSessionRepo;
    private final ContentReportRepository reportRepo;
    private final RewardService rewardService;
    private final ProblemRepository problemRepo;

    public AdminService(UserRepository userRepo, CourseRepository courseRepo,
                        QuizRepository quizRepo, MockExamRepository mockExamRepo,
                        PeerSessionRepository peerSessionRepo,
                        ContentReportRepository reportRepo,
                        RewardService rewardService,
                        ProblemRepository problemRepo) {
        this.userRepo = userRepo;
        this.courseRepo = courseRepo;
        this.quizRepo = quizRepo;
        this.mockExamRepo = mockExamRepo;
        this.peerSessionRepo = peerSessionRepo;
        this.reportRepo = reportRepo;
        this.rewardService = rewardService;
        this.problemRepo = problemRepo;
    }

    // ── Users ────────────────────────────────────────────────────────────────

    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepo.findAll(pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, User.AccountStatus status) {
        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        user.setStatus(status);
        return UserResponse.from(userRepo.save(user));
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, User.Role role) {
        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        user.setRole(role);
        return UserResponse.from(userRepo.save(user));
    }

    // ── Courses ──────────────────────────────────────────────────────────────

    public Page<Course> listAllCourses(Pageable pageable) {
        return courseRepo.findAll(pageable);
    }

    @Transactional
    public Course createCourse(Course course) {
        return courseRepo.save(course);
    }

    @Transactional
    public Course updateCourse(Long courseId, Course updates) {
        Course course = courseRepo.findById(courseId).orElseThrow(() -> ApiException.notFound("Course not found"));
        if (updates.getTitle() != null) course.setTitle(updates.getTitle());
        if (updates.getDescription() != null) course.setDescription(updates.getDescription());
        if (updates.getCategory() != null) course.setCategory(updates.getCategory());
        if (updates.getPointsPrice() > 0) course.setPointsPrice(updates.getPointsPrice());
        return courseRepo.save(course);
    }

    @Transactional
    public void toggleCourseActive(Long courseId, boolean active) {
        Course course = courseRepo.findById(courseId).orElseThrow(() -> ApiException.notFound("Course not found"));
        course.setActive(active);
        courseRepo.save(course);
    }

    // ── Disputes / Reports ───────────────────────────────────────────────────

    public Page<ContentReport> listReports(ContentReport.Status status, Pageable pageable) {
        if (status != null) return reportRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
        return reportRepo.findAll(pageable);
    }

    public Page<PeerSession> listDisputes(Pageable pageable) {
        return peerSessionRepo.findByStatusOrderByCreatedAtDesc(PeerSession.Status.DISPUTED, pageable);
    }

    @Transactional
    public PeerSession resolveDispute(Long sessionId, PeerSession.Status resolution) {
        PeerSession session = peerSessionRepo.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (session.getStatus() != PeerSession.Status.DISPUTED)
            throw ApiException.conflict("Session is not disputed");
        session.setStatus(resolution);
        return peerSessionRepo.save(session);
    }

    // ── Reward adjustments ───────────────────────────────────────────────────

    @Transactional
    public RewardTransaction adjustReward(Long adminId, Long userId, int amount, String reason) {
        String idem = "admin-adj-" + adminId + "-" + userId + "-" + Instant.now().toEpochMilli();
        if (amount > 0) {
            return rewardService.credit(userId, amount,
                    RewardTransaction.TransactionType.ADMIN_ADJUSTMENT, reason, idem, null);
        } else {
            return rewardService.debit(userId, Math.abs(amount),
                    RewardTransaction.TransactionType.ADMIN_REVERSAL, reason, idem, null);
        }
    }

    // ── Quizzes / Exams ──────────────────────────────────────────────────────

    public Page<Quiz> listAllQuizzes(Pageable pageable) {
        return quizRepo.findAll(pageable);
    }

    @Transactional
    public void toggleQuizActive(Long quizId, boolean active) {
        Quiz quiz = quizRepo.findById(quizId).orElseThrow(() -> ApiException.notFound("Quiz not found"));
        quiz.setActive(active);
        quizRepo.save(quiz);
    }

    public Page<MockExam> listAllMockExams(Pageable pageable) {
        return mockExamRepo.findAll(pageable);
    }

    @Transactional
    public void toggleMockExamActive(Long examId, boolean active) {
        MockExam exam = mockExamRepo.findById(examId).orElseThrow(() -> ApiException.notFound("Exam not found"));
        exam.setActive(active);
        mockExamRepo.save(exam);
    }

    // ── Problems ─────────────────────────────────────────────────────────────

    @Transactional
    public Problem createProblem(Problem problem) {
        return problemRepo.save(problem);
    }

    @Transactional
    public void toggleProblemActive(Long problemId, boolean active) {
        Problem p = problemRepo.findById(problemId).orElseThrow(() -> ApiException.notFound("Problem not found"));
        p.setActive(active);
        problemRepo.save(p);
    }
}
