package com.learnmate.backend.notes.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.notes.entity.Note;
import com.learnmate.backend.notes.service.NotesService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/notes")
public class NotesController {

    private final NotesService service;
    private final JwtUtil jwtUtil;

    public NotesController(NotesService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ApiResponse<Page<Note>> listPublicNotes(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listPublicNotes(subject, q, PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/my-notes")
    public ApiResponse<Page<Note>> myNotes(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getMyNotes(uid(auth), PageRequest.of(page, Math.min(size, 50))));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Note> upload(
            @RequestHeader("Authorization") String auth,
            @RequestPart("file") MultipartFile file,
            @RequestPart("title") String title,
            @RequestPart(value = "subject", required = false) String subject,
            @RequestPart(value = "topic", required = false) String topic,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "isPublic", required = false) String isPublic) throws IOException {
        boolean pub = "true".equalsIgnoreCase(isPublic);
        return ApiResponse.ok(service.uploadNote(uid(auth), file, title, subject, topic, description, pub));
    }

    @GetMapping("/{id}")
    public ApiResponse<Note> getNote(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) {
        return ApiResponse.ok(service.getVisibleNote(uid(auth), id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) throws Exception {
        Resource resource = service.downloadNote(uid(auth), id);
        Note note = service.getNote(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(note.getMimeType() != null ? note.getMimeType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + note.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) throws IOException {
        service.deleteNote(uid(auth), id);
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
