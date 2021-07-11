package com.example.androidkotlincoroutines.model

data class User(
    val name: String,
    val address: String
) {
    companion object {
        val DEFAULT = User("", "")
    }
}