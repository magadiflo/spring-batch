package dev.magadiflo.batch.app.repositories;

import dev.magadiflo.batch.app.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Integer> {
}
