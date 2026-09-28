package com.example.proyectofinal.routes

import com.example.proyectofinal.plugins.currentRole
import com.example.proyectofinal.plugins.currentUserId
import com.example.proyectofinal.service.NextHintResult
import com.example.proyectofinal.service.PedagogicalContentService
import com.example.proyectofinal.service.TheoryReadResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.pedagogicalContentRoutes(service: PedagogicalContentService) {
    routing {
        authenticate("auth-jwt") {
            get("/lessons/{id}/theory") {
                val lessonId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userId = call.currentUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val role = call.currentRole() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                when (val result = service.getTheory(lessonId, userId, role)) {
                    is TheoryReadResult.Success -> call.respond(result.response)
                    TheoryReadResult.Forbidden -> call.respond(HttpStatusCode.Forbidden, "Forbidden")
                    TheoryReadResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            post("/exercises/{id}/hints/next") {
                val exerciseId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userId = call.currentUserId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val role = call.currentRole() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                when (val result = service.revealNextHint(exerciseId, userId, role)) {
                    is NextHintResult.Success -> call.respond(result.response)
                    NextHintResult.Forbidden -> call.respond(HttpStatusCode.Forbidden, "Forbidden")
                    NextHintResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
