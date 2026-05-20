# Student Assistant App ER Diagram

## Full Logical ER Diagram

```mermaid
erDiagram
    USERS ||--|| USER_STATS : has
    USERS ||--o{ TASKS : creates
    USERS ||--o{ ROUTINES : schedules
    USERS ||--o{ ATTENDANCE_SUBJECTS : tracks
    USERS ||--o{ NOTES : writes
    USERS ||--o{ SYLLABUS_SUBJECTS : manages
    USERS ||--o{ SEMESTERS : owns
    USERS ||--o{ FOCUS_SESSIONS : completes
    USERS ||--o{ PASSWORD_RESETS : requests

    SYLLABUS_SUBJECTS ||--o{ SYLLABUS_TOPICS : contains
    SEMESTERS ||--o{ SEMESTER_COURSES : includes

    USERS {
        string user_id PK
        string full_name
        string email
        string password_hash
        string profile_image_url
        string auth_provider
        boolean is_email_verified
        timestamp created_at
        timestamp updated_at
        timestamp last_login_at
    }

    USER_STATS {
        string stats_id PK
        string user_id FK
        float current_cgpa
        int overall_attendance_percent
        float total_focus_hours
        int total_focus_minutes
        int today_focus_minutes
        int coins
        int streak
        string current_semester_name
        string last_task_title
        string last_task_category
        string last_task_date
        timestamp updated_at
    }

    TASKS {
        string task_id PK
        string user_id FK
        string title
        string category
        string due_date
        string priority
        string description
        boolean is_completed
        timestamp created_at
        timestamp updated_at
        timestamp completed_at
    }

    ROUTINES {
        string routine_id PK
        string user_id FK
        string day_of_week
        string subject_name
        string teacher_name
        string room
        string class_type
        string start_time
        string end_time
        boolean is_cancelled
        timestamp created_at
        timestamp updated_at
    }

    ATTENDANCE_SUBJECTS {
        string attendance_id PK
        string user_id FK
        string subject_name
        int total_classes
        int present_classes
        int absent_classes
        float attendance_percent
        timestamp created_at
        timestamp updated_at
    }

    NOTES {
        string note_id PK
        string user_id FK
        string subject_name
        string title
        string content
        string pdf_url
        string pdf_name
        timestamp created_at
        timestamp updated_at
    }

    SYLLABUS_SUBJECTS {
        string syllabus_subject_id PK
        string user_id FK
        string subject_name
        string subtitle
        string pdf_url
        int total_topics
        int completed_topics
        int progress_percent
        timestamp created_at
        timestamp updated_at
    }

    SYLLABUS_TOPICS {
        string topic_id PK
        string syllabus_subject_id FK
        string topic_name
        string subtitle
        string status
        boolean is_completed
        int sort_order
        timestamp created_at
        timestamp updated_at
    }

    SEMESTERS {
        string semester_id PK
        string user_id FK
        string semester_name
        int total_courses
        float total_credits
        float semester_gpa
        float cumulative_cgpa
        string standing
        timestamp created_at
        timestamp updated_at
    }

    SEMESTER_COURSES {
        string semester_course_id PK
        string semester_id FK
        string course_name
        float credits
        string grade_label
        float grade_point
        float weighted_point
        timestamp created_at
    }

    FOCUS_SESSIONS {
        string session_id PK
        string user_id FK
        int planned_duration_minutes
        int completed_duration_minutes
        int earned_coins
        boolean completed
        string level_label
        timestamp started_at
        timestamp completed_at
    }

    PASSWORD_RESETS {
        string reset_id PK
        string user_id FK
        string email
        string token
        timestamp requested_at
        timestamp expires_at
        boolean used
    }
```

## Firestore Structure

```text
users/{userId}
users/{userId}/stats/summary
users/{userId}/tasks/{taskId}
users/{userId}/routines/{routineId}
users/{userId}/attendanceSubjects/{attendanceId}
users/{userId}/notes/{noteId}
users/{userId}/syllabusSubjects/{subjectId}
users/{userId}/syllabusSubjects/{subjectId}/topics/{topicId}
users/{userId}/semesters/{semesterId}
users/{userId}/semesters/{semesterId}/courses/{courseId}
users/{userId}/focusSessions/{sessionId}
users/{userId}/passwordResets/{resetId}
```
