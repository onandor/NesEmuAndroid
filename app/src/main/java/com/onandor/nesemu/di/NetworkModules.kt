package com.onandor.nesemu.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Qualifier

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class SteamGridDB

@Module
@InstallIn(SingletonComponent::class)
class HttpClientModule {

    @Provides
    @SteamGridDB
    fun provideSGDBHttpClientFactory(): (String) -> HttpClient = { apiKey ->
        HttpClient(CIO) {
            install(DefaultRequest) {
                url {
                    host = "steamgriddb.com"
                    path("api/v2/")
                    protocol = URLProtocol.HTTPS
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Auth) {
                bearer {
                    sendWithoutRequest { true }
                    loadTokens {
                        BearerTokens(apiKey, null)
                    }
                }
            }
        }
    }
}