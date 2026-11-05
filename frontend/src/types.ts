// Single source of frontend types, aligned with the recreation API contract
// (analysis/recreation-architecture.md section 3.1).

export type UserRole = 'student' | 'instructor' | 'admin';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: UserRole[];
  enrolledCourses: string[];
  managedCourses: string[];
}

export type ContentType = 'video' | 'document' | 'assignment' | 'quiz' | 'reference_book';

export interface ModuleContent {
  id: string;
  title: string;
  type: ContentType;
  content: string;
  required: boolean;
}

export interface CourseModule {
  id: string;
  title: string;
  description: string;
  dueDate?: string;
  contents: ModuleContent[];
}

export interface Announcement {
  id: string;
  title: string;
  content: string;
  authorId: string;
  createdAt: string;
}

export interface DiscussionReply {
  id: string;
  content: string;
  authorId: string;
  isAnonymous: boolean;
  createdAt: string;
}

export interface Discussion {
  id: string;
  title: string;
  content: string;
  authorId: string;
  isAnonymous: boolean;
  createdAt: string;
  replies: DiscussionReply[];
}

export interface Course {
  id: string;
  title: string;
  description: string;
  adminId: string;
  enrolledStudents: string[];
  teachingAssistants: string[];
  modules: CourseModule[];
  announcements: Announcement[];
  discussions: Discussion[];
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  firstName: string;
  lastName: string;
  role: UserRole;
}
