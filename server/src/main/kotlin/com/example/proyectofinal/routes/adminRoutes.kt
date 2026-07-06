package com.example.proyectofinal.routes

import com.example.proyectofinal.models.AdminExerciseListResponse
import com.example.proyectofinal.models.AdminExerciseResponse
import com.example.proyectofinal.models.AdminLessonListResponse
import com.example.proyectofinal.models.AdminLessonResponse
import com.example.proyectofinal.models.CreateAdminCourseRequest
import com.example.proyectofinal.models.CreateAdminExerciseRequest
import com.example.proyectofinal.models.CreateAdminLessonRequest
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.RoleUpdateRequest
import com.example.proyectofinal.models.UpdateAdminCourseRequest
import com.example.proyectofinal.models.UpdateAdminExerciseRequest
import com.example.proyectofinal.models.UpdateUserRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.currentUserId
import com.example.proyectofinal.plugins.requireAdmin
import com.example.proyectofinal.service.AdminCourseMutationResult
import com.example.proyectofinal.service.AdminExerciseMutationResult
import com.example.proyectofinal.service.AdminLessonMutationResult
import com.example.proyectofinal.service.AdminLessonPatchRequest
import com.example.proyectofinal.service.CourseService
import com.example.proyectofinal.service.ExerciseService
import com.example.proyectofinal.service.FieldPatch
import com.example.proyectofinal.service.LessonService
import com.example.proyectofinal.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun Application.adminRoutes(
    userService: UserService,
    courseService: CourseService,
    lessonService: LessonService,
    exerciseService: ExerciseService
) {
    routing {
        authenticate("auth-jwt") {
            get("/admin/users") {
                if (!call.requireAdmin()) return@get

                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val query = call.request.queryParameters["query"]

                val safeSize = size.coerceIn(1, 100)

                call.respond(userService.listUsers(query = query, page = page, size = safeSize))
            }

            get("/admin/courses") {
                if (!call.requireAdmin()) return@get

                call.respond(courseService.getAllCoursesAdmin())
            }

            post("/admin/courses") {
                if (!call.requireAdmin()) return@post

                val authenticatedUserId = call.currentUserId()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val request = try {
                    call.receive<CreateAdminCourseRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }

                when (val result = courseService.adminCreateCourse(request, authenticatedUserId)) {
                    is AdminCourseMutationResult.Success -> call.respond(result.course)
                    is AdminCourseMutationResult.InvalidRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
                    AdminCourseMutationResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            put("/admin/courses/{id}") {
                if (!call.requireAdmin()) return@put

                val courseId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = try {
                    call.receive<UpdateAdminCourseRequest>()
                } catch (_: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }

                when (val result = courseService.adminUpdateCourse(courseId, request)) {
                    is AdminCourseMutationResult.Success -> call.respond(result.course)
                    is AdminCourseMutationResult.InvalidRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
                    AdminCourseMutationResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            delete("/admin/courses/{id}") {
                if (!call.requireAdmin()) return@delete

                val courseId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                if (!courseService.adminDeleteCourse(courseId)) {
                    return@delete call.respond(HttpStatusCode.NotFound)
                }

                call.respond(HttpStatusCode.NoContent)
            }

            get("/admin/lessons") {
                if (!call.requireAdmin()) return@get

                val hasCourseFilter = call.request.queryParameters.names().contains("courseId")
                val courseIdFilter = call.request.queryParameters["courseId"]

                val lessons = when {
                    !hasCourseFilter -> lessonService.listLessonsAdmin()
                    courseIdFilter.isNullOrBlank() -> lessonService.listStandaloneLessons()
                    else -> lessonService.getLessonsByCourseIdAdmin(courseIdFilter)
                }

                call.respond(AdminLessonListResponse(lessons.map(Lesson::toAdminLessonResponse)))
            }

            post("/admin/lessons") {
                if (!call.requireAdmin()) return@post

                val authenticatedUserId = call.currentUserId()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val request = try {
                    call.receive<CreateAdminLessonRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }

                when (val result = lessonService.adminCreateLesson(request, authenticatedUserId)) {
                    is AdminLessonMutationResult.Success -> call.respond(result.lesson.toAdminLessonResponse())
                    is AdminLessonMutationResult.InvalidRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
                    AdminLessonMutationResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            put("/admin/lessons/{id}") {
                if (!call.requireAdmin()) return@put

                val lessonId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val patch = try {
                    call.receive<JsonObject>().toAdminLessonPatchRequest()
                } catch (exception: Exception) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        exception.message ?: "Invalid lesson patch body"
                    )
                }

                when (val result = lessonService.adminUpdateLesson(lessonId, patch)) {
                    is AdminLessonMutationResult.Success -> call.respond(result.lesson.toAdminLessonResponse())
                    is AdminLessonMutationResult.InvalidRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
                    AdminLessonMutationResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            delete("/admin/lessons/{id}") {
                if (!call.requireAdmin()) return@delete

                val lessonId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                if (!lessonService.deleteLessonAdmin(lessonId)) {
                    return@delete call.respond(HttpStatusCode.NotFound)
                }

                call.respond(HttpStatusCode.NoContent)
            }

            get("/admin/exercises") {
                if (!call.requireAdmin()) return@get

                val hasLessonFilter = call.request.queryParameters.names().contains("lessonId")
                val lessonIdFilter = call.request.queryParameters["lessonId"]

                if (hasLessonFilter && lessonIdFilter.isNullOrBlank()) {
                    return@get call.respond(HttpStatusCode.BadRequest, "lessonId cannot be blank when provided")
                }

                val exercises = exerciseService.listExercisesAdmin(lessonIdFilter)
                call.respond(AdminExerciseListResponse(exercises.map(Exercise::toAdminExerciseResponse)))
            }

            post("/admin/exercises") {
                if (!call.requireAdmin()) return@post

                val request = try {
                    call.receive<CreateAdminExerciseRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }

                when (val result = exerciseService.adminCreateExercise(request)) {
                    is AdminExerciseMutationResult.Success -> call.respond(result.exercise.toAdminExerciseResponse())
                    is AdminExerciseMutationResult.InvalidRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
                    AdminExerciseMutationResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            put("/admin/exercises/{id}") {
                if (!call.requireAdmin()) return@put

                val exerciseId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = try {
                    call.receive<UpdateAdminExerciseRequest>()
                } catch (_: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }

                when (val result = exerciseService.adminUpdateExercise(exerciseId, request)) {
                    is AdminExerciseMutationResult.Success -> call.respond(result.exercise.toAdminExerciseResponse())
                    is AdminExerciseMutationResult.InvalidRequest -> call.respond(HttpStatusCode.BadRequest, result.message)
                    AdminExerciseMutationResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            delete("/admin/exercises/{id}") {
                if (!call.requireAdmin()) return@delete

                val exerciseId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                if (!exerciseService.adminDeleteExercise(exerciseId)) {
                    return@delete call.respond(HttpStatusCode.NotFound)
                }

                call.respond(HttpStatusCode.NoContent)
            }

            put("/admin/users/{id}/role") {
                if (!call.requireAdmin()) return@put

                val userId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<RoleUpdateRequest>()

                val role = UserRole.parse(request.role)
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid role")

                val updated = userService.updateUser(userId, UpdateUserRequest(role = role))
                    ?: return@put call.respond(HttpStatusCode.NotFound)

                call.respond(updated)
            }
        }
    }
}

private fun Lesson.toAdminLessonResponse(): AdminLessonResponse =
    AdminLessonResponse(
        id = id,
        courseId = courseId,
        creatorId = creatorId,
        title = title,
        theoryContent = theoryContent
    )

private fun Exercise.toAdminExerciseResponse(): AdminExerciseResponse =
    AdminExerciseResponse(
        id = id,
        lessonId = lessonId,
        question = question,
        options = options,
        correctAnswer = correctAnswer,
        type = type
    )

private fun JsonObject.toAdminLessonPatchRequest(): AdminLessonPatchRequest =
    AdminLessonPatchRequest(
        title = requiredStringPatch("title"),
        theoryContent = requiredStringPatch("theoryContent"),
        courseId = nullableStringPatch("courseId"),
        creatorId = nullableStringPatch("creatorId")
    )

private fun JsonObject.requiredStringPatch(fieldName: String): FieldPatch<String> {
    val element = this[fieldName] ?: return FieldPatch.Unchanged

    if (element == JsonNull) {
        throw IllegalArgumentException("$fieldName cannot be null")
    }

    val primitive = element as? JsonPrimitive
        ?: throw IllegalArgumentException("$fieldName must be a string")

    if (!primitive.isString) {
        throw IllegalArgumentException("$fieldName must be a string")
    }

    return FieldPatch.Present(primitive.content)
}

private fun JsonObject.nullableStringPatch(fieldName: String): FieldPatch<String?> {
    val element = this[fieldName] ?: return FieldPatch.Unchanged

    if (element == JsonNull) {
        return FieldPatch.Present(null)
    }

    val primitive = element as? JsonPrimitive
        ?: throw IllegalArgumentException("$fieldName must be a string or null")

    if (!primitive.isString) {
        throw IllegalArgumentException("$fieldName must be a string or null")
    }

    return FieldPatch.Present(primitive.content)
}
