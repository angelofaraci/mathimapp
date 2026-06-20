package com.example.proyectofinal.di

interface TokenStore {
    var accessToken: String?
}

class InMemoryTokenStore : TokenStore {
    override var accessToken: String? = null
}
