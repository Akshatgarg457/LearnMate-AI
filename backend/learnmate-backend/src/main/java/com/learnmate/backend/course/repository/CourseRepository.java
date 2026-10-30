package com.learnmate.backend.course.repository;
import com.learnmate.backend.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseRepository extends JpaRepository<Course, Long> {
    Page<Course> findByActiveTrue(Pageable p);
    Page<Course> findByActiveTrueAndCategory(String category, Pageable p);
    Page<Course> findByActiveTrueAndCourseType(Course.CourseType type, Pageable p);
}
