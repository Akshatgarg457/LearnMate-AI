package com.learnmate.backend.notes.repository;
import com.learnmate.backend.notes.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
public interface NoteRepository extends JpaRepository<Note, Long> {
    Page<Note> findByIsPublicTrue(Pageable p);
    Page<Note> findByIsPublicTrueAndSubject(String subject, Pageable p);
    Page<Note> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable p);
    Optional<Note> findByIdAndOwnerId(Long id, Long ownerId);
    @Query("SELECT n FROM Note n WHERE n.isPublic = true AND (LOWER(n.title) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(n.description) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Note> searchPublic(String q, Pageable p);
}
