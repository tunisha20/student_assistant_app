package com.example.studentassistantappv1.data;
import java.util.HashMap;

import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseApi {

    // --- Authentication & Constants ---
    String apiKey = "sb_publishable_IkueCnQWERth0MDZ9lqfzg_xmikaGpJ";

    @POST("auth/v1/signup")
    Call<ResponseBody> signUp(
            @Header("apikey") String apiKey,
            @Header("Content-Type") String contentType,
            @Body Map<String, Object> body
    );

    @POST("auth/v1/token?grant_type=password")
    Call<ResponseBody> login(
            @Header("apikey") String apiKey,
            @Body Map<String, Object> body
    );

    @GET("rest/v1/profiles")
    Call<ResponseBody> getProfileData(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("select") String select,
            @Query("id") String userIdFilter
    );

    @PATCH("rest/v1/profiles")
    Call<ResponseBody> updateProfile(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Header("Content-Type") String contentType,
            @Query("id") String idFilter,
            @Body Map<String, Object> profileData
    );
    @GET("rest/v1/syllabus")
    Call<ResponseBody> getSyllabus(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("select") String select,
            @Query("user_id") String userIdFilter
    );

    @POST("rest/v1/syllabus")
    Call<ResponseBody> insertSyllabus(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Header("Content-Type") String contentType,
            @Header("Prefer") String prefer,
            @Body Map<String, Object> body
    );
        // ১. টাস্ক লিস্ট আনা
        @GET("rest/v1/tasks")
        Call<ResponseBody> getTasks(
                @Header("apikey") String apiKey,
                @Header("Authorization") String auth,
                @Query("select") String select,
                @Query("user_id") String userIdFilter
        );





        // ৩. টাস্ক আপডেট করা (যেমন: কমপ্লিট স্ট্যাটাস)
        @PATCH("rest/v1/tasks")
        Call<ResponseBody> updateTask(
                @Header("apikey") String apiKey,
                @Header("Authorization") String auth,
                @Query("id") String idFilter,
                @Body Map<String, Object> body
        );

    @GET("rest/v1/attendance")
    Call<ResponseBody> getAttendanceData(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("select") String select,
            @Query("user_id") String userIdFilter
    );

    @POST("rest/v1/attendance")
    Call<ResponseBody> insertAttendance(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Header("Content-Type") String contentType,
            @Header("Prefer") String prefer,
            @Body Map<String, Object> attendanceData
    );

    @PATCH("rest/v1/attendance")
    Call<ResponseBody> updateAttendance(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Header("Content-Type") String contentType,
            @Query("id") String idFilter,
            @Body Map<String, Object> updateData
    );

    @GET("rest/v1/notes")
    Call<ResponseBody> getNotes(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("select") String select,
            @Query("user_id") String userIdFilter
    );

    @POST("rest/v1/notes")
    Call<ResponseBody> insertNote(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Header("Content-Type") String contentType,
            @Header("Prefer") String prefer,
            @Body Map<String, Object> noteData
    );

    @DELETE("rest/v1/notes")
    Call<ResponseBody> deleteNote(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("id") String noteIdFilter
    );

    @GET("rest/v1/schedules")
    Call<ResponseBody> getSchedules(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("select") String select,
            @Query("user_id") String userIdFilter
    );

    @POST("rest/v1/tasks")
    Call<ResponseBody> insertTask(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Header("Content-Type") String contentType,
            @Header("Prefer") String prefer,
            @Body Map<String, Object> taskData
    );
    // SupabaseApi.java-তে order প্যারামিটার যোগ করুন
    @GET("rest/v1/tasks")
    Call<ResponseBody> getTasks(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("select") String select,
            @Query("user_id") String userIdFilter,
            @Query("order") String order // এটি যোগ করুন
    );
    @POST("rest/v1/study_sessions")
    Call<ResponseBody> insertStudySession(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Header("Content-Type") String contentType,
            @Body Map<String, Object> sessionData
    );

    @POST("rest/v1/cgpa_history")
    Call<ResponseBody> insertCgpaHistory(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Header("Content-Type") String contentType,
            @Body Map<String, Object> historyData
    );

    @GET("rest/v1/cgpa_history")
    Call<ResponseBody> getCgpaHistory(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("select") String columns,
            @Query("user_id") String userIdFilter
    );
    // নির্দিষ্ট দিনের রুটিন আনা
    @GET("rest/v1/routines")
    Call<ResponseBody> getRoutines(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("select") String select,      // যেমন: "*"
            @Query("day_of_week") String day,    // যেমন: "eq.SAT"
            @Query("user_id") String userId      // যেমন: "eq.uuid"
    );

    @POST("rest/v1/routines")
    Call<ResponseBody> insertRoutine(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Header("Content-Type") String contentType, // application/json
            @Header("Prefer") String prefer,           // return=representation
            @Body Map<String, Object> body
    );
    @PATCH("rest/v1/routines")
    Call<ResponseBody> updateRoutine(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("id") String idFilter,
            @Body Map<String, Object> body
    );
    @POST("rest/v1/focus_sessions")
    Call<ResponseBody> saveFocusSession(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authToken,
            @Body HashMap<String, Object> body // 👈 নিশ্চিত করুন এখানে HashMap আছে
    );
    @GET("rest/v1/routine")
    Call<ResponseBody> getRoutine(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authToken,
            @Query("select") String select,
            @Query("user_id") String userId,
            @Query("day") String day // বর্তমান দিন অনুযায়ী ফিল্টার করতে (e.g., eq.Monday)
    );
}
