package com.learnmate.backend.notes.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notes", indexes = {
        @Index(name = "idx_notes_owner", columnList = "owner_id"),
        @Index(name = "idx_notes_subject", columnList = "subject"),
        @Index(name = "idx_notes_public", columnList = "is_public")
})
public class Note {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "email", "role", "status", "bio", "avatarUrl", "timezone", "createdAt", "updatedAt", "hibernateLazyInitializer", "handler"})
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String subject;

    @Column(length = 100)
    private String topic;

    /** Internal storage path — never returned raw in API responses */
    @Column(name = "storage_path", length = 512)
    @JsonIgnore
    private String storagePath;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "download_count", nullable = false)
    private int downloadCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public void setOwner(User u) { this.owner = u; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }
    public String getTopic() { return topic; }
    public void setTopic(String t) { this.topic = t; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String s) { this.storagePath = s; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String f) { this.originalFilename = f; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String m) { this.mimeType = m; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long f) { this.fileSizeBytes = f; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean p) { this.isPublic = p; }
    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int d) { this.downloadCount = d; }
    public Instant getCreatedAt() { return createdAt; }
}
