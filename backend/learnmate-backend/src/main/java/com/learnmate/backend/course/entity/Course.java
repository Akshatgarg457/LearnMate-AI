package com.learnmate.backend.course.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "courses", indexes = {
        @Index(name = "idx_course_type", columnList = "course_type"),
        @Index(name = "idx_course_active", columnList = "active")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false, length = 20)
    private CourseType courseType = CourseType.INTERNAL;

    @Column(name = "points_price", nullable = false)
    private int pointsPrice = 0;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(name = "provider_name", length = 200)
    private String providerName;

    @Column(name = "provider_course_id", length = 200)
    private String providerCourseId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public enum CourseType { INTERNAL, PARTNER }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }
    public CourseType getCourseType() { return courseType; }
    public void setCourseType(CourseType t) { this.courseType = t; }
    public int getPointsPrice() { return pointsPrice; }
    public void setPointsPrice(int p) { this.pointsPrice = p; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String u) { this.thumbnailUrl = u; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String p) { this.providerName = p; }
    public String getProviderCourseId() { return providerCourseId; }
    public void setProviderCourseId(String p) { this.providerCourseId = p; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public Instant getCreatedAt() { return createdAt; }
}
