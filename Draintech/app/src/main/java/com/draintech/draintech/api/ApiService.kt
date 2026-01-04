package com.draintech.draintech.api

import com.draintech.draintech.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    // Login: envía email y password, recibe usuario o error
    @POST("login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<User>

    // Registro: envía username, email y password, recibe usuario o error
    @POST("register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<User>
}

// --- Requests para login y registro ---
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)
