package com.learnmate.backend.peer.repository;
import com.learnmate.backend.peer.entity.PeerSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface PeerSessionRepository extends JpaRepository<PeerSession, Long> {
    Page<PeerSession> findByLearnerIdOrderByCreatedAtDesc(Long learnerId, Pageable p);
    Page<PeerSession> findByTeacherIdOrderByCreatedAtDesc(Long teacherId, Pageable p);
    Page<PeerSession> findByStatusOrderByCreatedAtDesc(PeerSession.Status status, Pageable p);
    Optional<PeerSession> findByIdAndLearnerId(Long id, Long learnerId);
    Optional<PeerSession> findByIdAndTeacherId(Long id, Long teacherId);
    @Query("SELECT s FROM PeerSession s WHERE (s.learner.id = :userId OR s.teacher.id = :userId) ORDER BY s.createdAt DESC")
    Page<PeerSession> findByParticipant(Long userId, Pageable p);

    /** Serializes acceptance so two teachers cannot both create a meeting. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PeerSession s WHERE s.id = :id")
    Optional<PeerSession> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PeerSession s WHERE s.id = :id AND s.teacher.id = :teacherId")
    Optional<PeerSession> findByIdAndTeacherIdForUpdate(@Param("id") Long id, @Param("teacherId") Long teacherId);
}
