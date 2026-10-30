package com.learnmate.backend.notes.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.notes.entity.Note;
import com.learnmate.backend.notes.repository.NoteRepository;
import com.learnmate.backend.progress.entity.LearningActivity;
import com.learnmate.backend.progress.service.ProgressService;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class NotesService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", "text/markdown"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final NoteRepository noteRepo;
    private final UserRepository userRepo;
    private final ProgressService progressService;
    private final Path uploadDir;

    public NotesService(NoteRepository noteRepo, UserRepository userRepo,
                        ProgressService progressService,
                        @Value("${app.storage.upload-dir}") String uploadDirPath) {
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
        this.progressService = progressService;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try { Files.createDirectories(this.uploadDir); } catch (IOException e) { /* log */ }
    }

    public Page<Note> listPublicNotes(String subject, String query, Pageable pageable) {
        if (query != null && !query.isBlank()) return noteRepo.searchPublic(query.trim(), pageable);
        if (subject != null) return noteRepo.findByIsPublicTrueAndSubject(subject, pageable);
        return noteRepo.findByIsPublicTrue(pageable);
    }

    public Page<Note> getMyNotes(Long userId, Pageable pageable) {
        return noteRepo.findByOwnerIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public Note uploadNote(Long userId, MultipartFile file, String title, String subject,
                           String topic, String description, boolean isPublic) throws IOException {
        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));

        if (file.isEmpty()) throw ApiException.badRequest("File is empty");
        if (file.getSize() > MAX_FILE_SIZE) throw ApiException.badRequest("File exceeds maximum size of 10MB");

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw ApiException.badRequest("File type not allowed. Permitted: PDF, images, Word, plain text");
        }

        // Store with random name to prevent path traversal / overwrite
        String ext = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString() + ext;
        Path targetPath = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        Note note = new Note();
        note.setOwner(user);
        note.setTitle(title);
        note.setSubject(subject);
        note.setTopic(topic);
        note.setDescription(description);
        note.setStoragePath(storedName); // relative path only — never expose full disk path
        note.setOriginalFilename(file.getOriginalFilename());
        note.setMimeType(mimeType);
        note.setFileSizeBytes(file.getSize());
        note.setPublic(isPublic);

        note = noteRepo.save(note);
        progressService.recordActivity(userId, LearningActivity.ActivityType.NOTE_ADDED, note.getId(), 5);
        return note;
    }

    @Transactional
    public Resource downloadNote(Long userId, Long noteId) throws MalformedURLException {
        Note note = noteRepo.findById(noteId).orElseThrow(() -> ApiException.notFound("Note not found"));

        boolean isOwner = note.getOwner().getId().equals(userId);
        if (!isOwner && !note.isPublic()) throw ApiException.forbidden("Access denied");

        Path filePath = uploadDir.resolve(note.getStoragePath()).normalize();
        // Security: ensure resolved path is inside upload dir (prevent path traversal)
        if (!filePath.startsWith(uploadDir)) throw ApiException.forbidden("Invalid file path");

        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) throw ApiException.notFound("File not found on server");

        note.setDownloadCount(note.getDownloadCount() + 1);
        noteRepo.save(note);
        return resource;
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) throws IOException {
        Note note = noteRepo.findByIdAndOwnerId(noteId, userId)
                .orElseThrow(() -> ApiException.notFound("Note not found"));
        Path filePath = uploadDir.resolve(note.getStoragePath()).normalize();
        Files.deleteIfExists(filePath);
        noteRepo.delete(note);
    }

    /** Metadata of a note, only if it is public or owned by the caller. */
    @Transactional(readOnly = true)
    public Note getVisibleNote(Long userId, Long noteId) {
        Note note = getNote(noteId);
        if (!note.isPublic() && !note.getOwner().getId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        return note;
    }

    public Note getNote(Long noteId) {
        return noteRepo.findById(noteId).orElseThrow(() -> ApiException.notFound("Note not found"));
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
