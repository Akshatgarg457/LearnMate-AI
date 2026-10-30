package com.learnmate.backend.community.repository;
import com.learnmate.backend.community.entity.ContentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {
    Page<ContentReport> findByStatusOrderByCreatedAtDesc(ContentReport.Status status, Pageable p);
    boolean existsByReporterIdAndContentTypeAndContentId(Long reporterId, ContentReport.ContentType type, Long contentId);
}
