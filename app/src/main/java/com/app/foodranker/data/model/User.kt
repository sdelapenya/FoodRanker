package com.app.foodranker.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val city: String = "",
    val website: String = "",
    val isPremium: Boolean = false,
    val xp: Int = 0,
    val level: Int = 1,
    val badges: List<String> = emptyList(),
    val referralCount: Int = 0,
    val referredByUserId: String = "",
    val createdAt: Long = 0L,
    // Solo Admin SDK puede escribirlo (ver firestore.rules). Sin prefijo "is" a propósito:
    // Firestore le quita el prefijo a los getters booleanos de Kotlin (isPremium -> premium),
    // y nombrarlo así evita repetir ese bug con un campo nuevo.
    val banned: Boolean = false
)
