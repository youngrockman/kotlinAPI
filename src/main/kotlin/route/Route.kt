package com.example.rout

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.route.DataRepository
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
data class Sneaker(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val category: String,
    val isPopular: Boolean = false,
    val isFavorite: Boolean = false,
    val quantity: Int
)

@Serializable
data class User(
    val userId: Int,
    val userName: String,
    val email: String,
    val password: String,
    val favoriteSneakerIds: List<Int> = emptyList(),
    val cart: List<Int> = emptyList()
)

@Serializable
data class RegistrationResponse(
    val token: String,
    val userId: Int,
    val userName: String,
    val email: String
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


            val user = DataRepository.userList.firstOrNull {
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

fun Route.sneakersRoute() {
    get("/allSneakers") {
        call.respond(DataRepository.sneakerList)
    }

    get("/sneakers/popular") {
        val popularSneakers = DataRepository.sneakerList.filter { it.isPopular }
        call.respond(popularSneakers)
    }

    get("/sneakers/{category}") {
        val category = call.parameters["category"] ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            ErrorResponse("Category parameter is required", 400)
        )

        val filtered = DataRepository.sneakerList.filter {
            it.category.equals(category, ignoreCase = true)
        }
        call.respond(filtered)
    }
}

fun Route.favoritesRoute() {
    authenticate("auth-jwt") {
        get("/favorites") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("userId", Int::class) ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse("Unauthorized", 401)
            )

            val user = DataRepository.userList.find { it.userId == userId }
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("User not found", 404)
                )

            val favorites = DataRepository.sneakerList.filter { it.id in user.favoriteSneakerIds }
            call.respond(favorites)
        }

        post("/favorites/{sneakerId}") {
            val sneakerId = call.parameters["sneakerId"]?.toIntOrNull() ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Invalid sneaker ID", 400)
            )

            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("userId", Int::class) ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse("Unauthorized", 401)
            )

            val userIndex = DataRepository.userList.indexOfFirst { it.userId == userId }
            if (userIndex == -1) return@post call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("User not found", 404)
            )


            if (!DataRepository.sneakerList.any { it.id == sneakerId }) return@post call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("Sneaker not found", 404)
            )


            val currentFavorites = DataRepository.userList[userIndex].favoriteSneakerIds
            if (currentFavorites.contains(sneakerId)) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("Sneaker already in favorites", 409))
                return@post
            }


            val updatedUser = DataRepository.userList[userIndex].copy(
                favoriteSneakerIds = currentFavorites + sneakerId
            )
            DataRepository.userList[userIndex] = updatedUser

            call.respond(HttpStatusCode.OK, mapOf("message" to "Sneaker added to favorites"))
        }
        delete("/favorites/{sneakerId}") {
            val sneakerId = call.parameters["sneakerId"]?.toIntOrNull() ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Invalid sneaker ID", 400)
            )

            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("userId", Int::class) ?: return@delete call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse("Unauthorized", 401)
            )

            val userIndex = DataRepository.userList.indexOfFirst { it.userId == userId }
            if (userIndex == -1) return@delete call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("User not found", 404)
            )

            val updatedUser = DataRepository.userList[userIndex].copy(
                favoriteSneakerIds = DataRepository.userList[userIndex].favoriteSneakerIds - sneakerId
            )
            DataRepository.userList[userIndex] = updatedUser

            call.respond(mapOf("message" to "Sneaker removed from favorites"))
        }
    }
}


fun Route.cartRoute() {
    authenticate("auth-jwt") {
        post("/cart/{sneakerId}") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getClaim("userId", Int::class)!!
            val sneakerId = call.parameters["sneakerId"]?.toIntOrNull() ?: return@post call.respond(
                HttpStatusCode.BadRequest, ErrorResponse("Invalid sneaker ID", 400)
            )

            val userIndex = DataRepository.userList.indexOfFirst { it.userId == userId }
            val user = DataRepository.userList[userIndex]
            val currentCart = user.cart.toMutableList()
            currentCart.add(sneakerId)

            DataRepository.userList[userIndex] = user.copy(cart = currentCart)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Added to cart"))
        }


        get("/cart") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getClaim("userId", Int::class)!!
            val user = DataRepository.userList.first { it.userId == userId }


            val itemCounts = user.cart.groupingBy { it }.eachCount()

            val items = itemCounts.mapNotNull { (id, count) ->
                DataRepository.sneakerList.find { it.id == id }?.copy(quantity = count)
            }

            call.respond(items)
        }

        delete("/cart/{sneakerId}") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getClaim("userId", Int::class)!!
            val sneakerId = call.parameters["sneakerId"]?.toIntOrNull() ?: return@delete call.respond(
                HttpStatusCode.BadRequest, ErrorResponse("Invalid sneaker ID", 400)
            )

            val userIndex = DataRepository.userList.indexOfFirst { it.userId == userId }
            val updatedCart = DataRepository.userList[userIndex].cart - sneakerId
            DataRepository.userList[userIndex] = DataRepository.userList[userIndex].copy(cart = updatedCart)
            call.respond(mapOf("message" to "Removed from cart"))
        }

        get("/cart/total") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getClaim("userId", Int::class)!!
            val user = DataRepository.userList.first { it.userId == userId }

            val items = user.cart.mapNotNull { id ->
                DataRepository.sneakerList.find { it.id == id }
            }

            val total = items.sumOf { it.price }
            val delivery = if (total > 500) 0.0 else 60.0
            val finalTotal = total + delivery

            call.respond(mapOf(
                "items" to items,
                "total" to total,
                "delivery" to delivery,
                "finalTotal" to finalTotal
            ))
        }

        delete("/cart/remove-all/{sneakerId}") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getClaim("userId", Int::class)!!
            val sneakerId = call.parameters["sneakerId"]?.toIntOrNull() ?: return@delete call.respond(
                HttpStatusCode.BadRequest, ErrorResponse("Invalid sneaker ID", 400)
            )

            val userIndex = DataRepository.userList.indexOfFirst { it.userId == userId }
            val updatedCart = DataRepository.userList[userIndex].cart.filter { it != sneakerId }
            DataRepository.userList[userIndex] = DataRepository.userList[userIndex].copy(cart = updatedCart)
            call.respond(mapOf("message" to "All items removed from cart"))
        }

        put("/cart/update-quantity") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.getClaim("userId", Int::class)!!
            val productId = call.request.queryParameters["productId"]?.toIntOrNull()
            val quantity = call.request.queryParameters["quantity"]?.toIntOrNull()

            if (productId == null || quantity == null || quantity < 0) {
                return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid productId or quantity", 400)
                )
            }

            val userIndex = DataRepository.userList.indexOfFirst { it.userId == userId }
            if (userIndex == -1) return@put call.respond(
                HttpStatusCode.NotFound, ErrorResponse("User not found", 404)
            )

            val currentCart = DataRepository.userList[userIndex].cart.toMutableList()


            val updatedCart = currentCart.filter { it != productId }.toMutableList()


            repeat(quantity) {
                updatedCart.add(productId)
            }

            DataRepository.userList[userIndex] = DataRepository.userList[userIndex].copy(cart = updatedCart)

            call.respond(HttpStatusCode.OK, mapOf("message" to "Cart quantity updated"))
        }
    }
}

fun Route.searchSneakersRoute() {
    get("/sneakers/search") {
        val query = call.request.queryParameters["query"]?.lowercase() ?: ""
        val result = DataRepository.sneakerList.filter {
            it.name.lowercase().contains(query) || it.description.lowercase().contains(query)
        }
        call.respond(result)
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