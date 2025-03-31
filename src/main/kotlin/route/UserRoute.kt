package com.example.rout

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class User(
    val userId: Int,
    val userName: String,
    val email: String,
    val password: String
)

@Serializable
data class CreateUserRequest(
    val userName: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Int,
    val userName: String,
    val email: String
)

@Serializable
data class ErrorResponse(
    val message: String,
    val errorCode: Int
)

val userList = mutableListOf<User>()

fun Route.authRoute() {
    post("/login") {
        try {
            val request = call.receive<LoginRequest>()

            val user = userList.firstOrNull {
                it.email == request.email && it.password == request.password
            } ?: run {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid email or password", HttpStatusCode.Unauthorized.value)
                )
                return@post
            }

            val token = generateJWTToken(call.application, user)
            call.respond(
                HttpStatusCode.OK,
                AuthResponse(
                    token = token,
                    userId = user.userId,
                    userName = user.userName,
                    email = user.email
                )
            )

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Invalid request format", HttpStatusCode.BadRequest.value)
            )
        }
    }

    post("/registration") {
        try {
            val request = call.receive<CreateUserRequest>()

            if (userList.any { it.email == request.email }) {
                call.respond(
                    HttpStatusCode.Conflict,
                    ErrorResponse("Email already exists", HttpStatusCode.Conflict.value)
                )
                return@post
            }

            val newUser = User(
                userId = userList.size + 1,
                userName = request.userName,
                email = request.email,
                password = request.password
            )

            userList.add(newUser)
            val token = generateJWTToken(call.application, newUser)

            call.respond(
                HttpStatusCode.Created,
                AuthResponse(
                    token = token,
                    userId = newUser.userId,
                    userName = newUser.userName,
                    email = newUser.email
                )
            )

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Invalid request format", HttpStatusCode.BadRequest.value)
            )
        }
    }

    authenticate("auth-jwt") {
        get("/profile/{userId}") {
            val userId = call.parameters["userId"]?.toIntOrNull()
            val user = userId?.let { id -> userList.firstOrNull { it.userId == id } }

            if (user != null) {
                call.respond(user)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("User not found", HttpStatusCode.NotFound.value)
                )
            }
        }
    }
}

private fun generateJWTToken(application: Application, user: User): String {
    val config = application.environment.config
    return JWT.create()
        .withAudience(config.property("jwt.audience").getString())
        .withIssuer(config.property("jwt.issuer").getString())
        .withClaim("userId", user.userId)
        .withClaim("userName", user.userName)
        .withClaim("email", user.email)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
        .sign(Algorithm.HMAC256(config.property("jwt.secret").getString()))
}