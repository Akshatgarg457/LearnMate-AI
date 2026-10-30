package com.learnmate.backend.course.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.course.entity.*;
import com.learnmate.backend.course.service.CourseService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService service;
    private final JwtUtil jwtUtil;

    public CourseController(CourseService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ApiResponse<Page<Course>> listCourses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Course.CourseType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listCourses(category, type, PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/{id}")
    public ApiResponse<Course> getCourse(@PathVariable Long id) {
        return ApiResponse.ok(service.getCourse(id));
    }

    @PostMapping("/{id}/redeem")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseRedemption> redeem(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) {
        return ApiResponse.ok(service.redeem(uid(auth), id));
    }

    @GetMapping("/my-courses")
    public ApiResponse<Page<CourseRedemption>> myCourses(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getMyCourses(uid(auth), PageRequest.of(page, Math.min(size, 50))));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
