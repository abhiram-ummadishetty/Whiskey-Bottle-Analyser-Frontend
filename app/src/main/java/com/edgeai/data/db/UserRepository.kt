package com.edgeai.data.db

import java.security.MessageDigest
import java.security.SecureRandom

class UserRepository(private val dao: UserDao) {
    suspend fun signUp(first: String, last: String, password: String): Result<Long> = runCatching {
        require(first.isNotBlank() && last.isNotBlank()) { "Name required" }
        require(password.length >= 6) { "Password must be at least 6 characters" }
        if (dao.findByName(first.trim(), last.trim()) != null) error("User already exists")
        val salt = randomSalt()
        val hash = hash(password, salt)
        dao.insert(User(firstName = first.trim(), lastName = last.trim(),
            passwordSalt = salt, passwordHash = hash))
    }

    suspend fun login(first: String, last: String, password: String): Result<User> = runCatching {
        val u = dao.findByName(first.trim(), last.trim()) ?: error("No such user")
        if (hash(password, u.passwordSalt) != u.passwordHash) error("Wrong password")
        u
    }

    suspend fun byId(id: Long): User? = dao.findById(id)

    private fun randomSalt(): String {
        val b = ByteArray(16); SecureRandom().nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    private fun hash(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest((salt + password).toByteArray()).joinToString("") { "%02x".format(it) }
    }
}