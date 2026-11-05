package com.edunexus.user;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByEnrolledCoursesContaining(String courseId);

    List<User> findByManagedCoursesContaining(String courseId);
}
