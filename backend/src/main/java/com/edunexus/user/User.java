package com.edunexus.user;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Mirrors the reference `users` collection (database-schema.md section 1). */
@Document(collection = "users")
public class User {
    @Id
    public String id;

    @Indexed(unique = true)
    public String email;

    public String passwordHash;
    public String firstName;
    public String lastName;
    public Set<Role> roles = new LinkedHashSet<>();
    public List<String> enrolledCourses = new ArrayList<>();
    public List<String> managedCourses = new ArrayList<>();

    @CreatedDate
    public Instant createdAt;

    @LastModifiedDate
    public Instant updatedAt;
}
