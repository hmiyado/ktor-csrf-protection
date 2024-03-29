package io.github.hmiyado.ktor.csrfprotection

import io.kotest.assertions.fail
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.ktor.client.request.cookie
import io.ktor.client.request.post
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.setCookie
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.header
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking

private const val CLIENT_SESSION = "client_session"
private const val X_CSRF_TOKEN = "X-CSRF-TOKEN"

class SessionCsrfTest : DescribeSpec() {

    lateinit var testApplicationEngine: TestApplicationEngine

    private val sessionStorage: SessionStorageMemory = SessionStorageMemory()

    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        testApplicationEngine = TestApplicationEngine().apply {
            start()
            application.install(Routing) {
                get("/") { call.respond(HttpStatusCode.OK) }
                get("/not-csrf") { call.respond(HttpStatusCode.OK) }
                post("/") { call.respond(HttpStatusCode.OK) }
                post("/not-csrf") { call.respond(HttpStatusCode.OK) }
            }
            application.install(Sessions) {
                cookie<ClientSession>(CLIENT_SESSION, storage = sessionStorage) {
                }
                header<CsrfTokenSession>(X_CSRF_TOKEN, storage = sessionStorage) {
                }
            }
            application.install(Csrf) {
                requestFilter { httpMethod, path ->
                    httpMethod == HttpMethod.Post && path == "/"
                }
                session<ClientSession> {
                    onFail { respond(HttpStatusCode.BadRequest) }
                }
            }
        }
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        runBlocking {
            sessionStorage.invalidate(CLIENT_SESSION)
            sessionStorage.invalidate(X_CSRF_TOKEN)
        }
        testApplicationEngine.stop(0, 0)
    }

    init {
        describe("request filter") {
            it("should not check csrf request that doesn't match HttpMethod") {
                testApplicationEngine.get("/").run {
                    response shouldHaveStatus HttpStatusCode.OK
                }
            }
            it("should not check csrf request that doesn't match path") {
                testApplicationEngine.post("/not-csrf").run {
                    response shouldHaveStatus HttpStatusCode.OK
                }
            }
            it("should not check csrf request that doesn't match path and method") {
                testApplicationEngine.get("/not-csrf").run {
                    response shouldHaveStatus HttpStatusCode.OK
                }
            }
        }
        describe("no or invalid client session, no or invalid csrf token") {
            it("should fail with no client session") {
                testApplicationEngine.post("/").run {
                    response shouldHaveStatus HttpStatusCode.BadRequest
                }
            }
            it("should fail with no client session but only empty cookie") {
                testApplicationEngine.post("/") {
                    addHeader("Cookie", "client_session=")
                }.run {
                    response shouldHaveStatus HttpStatusCode.BadRequest
                }
            }
        }
        describe("valid client session, no or invalid csrf token") {
            it("should fail with valid client session but no or invalid csrf token") {
                testApplicationEngine.stop(0, 0)

                testApplication {
                    install(Routing) {
                        post("/acquire_session") {
                            val clientSession = ClientSession("session")
                            call.sessions.set(clientSession)
                            call.respond(HttpStatusCode.OK)
                        }
                        post("/") {
                            val session = call.sessions.get<ClientSession>()
                            if (session == null) {
                                call.respond(HttpStatusCode.BadRequest)
                                return@post
                            }
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                    install(Sessions) {
                        cookie<ClientSession>(CLIENT_SESSION, storage = SessionStorageMemory()) {}
                        header<CsrfTokenSession>(X_CSRF_TOKEN, storage = SessionStorageMemory()) {}
                    }
                    install(Csrf) {
                        requestFilter { httpMethod, path ->
                            httpMethod == HttpMethod.Post && path == "/"
                        }
                        session<ClientSession> {
                            onFail { respond(HttpStatusCode.BadRequest) }
                        }
                    }

                    val responseAcquireSession = client.post("/acquire_session") {}
                    responseAcquireSession shouldHaveStatus HttpStatusCode.OK
                    val cookie = responseAcquireSession.setCookie().firstOrNull() ?: fail("no set-cookie")
                    val responseRoot1 = client.post("/") {
                        // no csrf token
                        cookie(cookie.name, cookie.value)
                    }
                    responseRoot1 shouldHaveStatus HttpStatusCode.BadRequest
                    val responseRoot2 = client.post("/") {
                        cookie(cookie.name, cookie.value)
                        // invalid csrf token
                        headers.append(X_CSRF_TOKEN, "invalid_csrf_token")
                    }
                    responseRoot2 shouldHaveStatus HttpStatusCode.BadRequest
                }
            }
        }
        describe("valid client session, valid csrf token") {
            it("should be success with valid csrf token session, valid client session") {
                testApplicationEngine.stop(0, 0)

                testApplication {
                    install(Sessions) {
                        cookie<ClientSession>(CLIENT_SESSION, storage = SessionStorageMemory()) {}
                        header<CsrfTokenSession>(X_CSRF_TOKEN, storage = SessionStorageMemory()) {}
                    }
                    routing {
                        post("/acquire_session") {
                            val clientSession = ClientSession("session")
                            call.sessions.set(clientSession)
                            call.respond(HttpStatusCode.OK)
                        }
                        post("/") {
                            val session = call.sessions.get<ClientSession>()
                            if (session == null) {
                                call.respond(HttpStatusCode.Forbidden)
                                return@post
                            }
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                    install(Csrf) {
                        requestFilter { httpMethod, path ->
                            httpMethod == HttpMethod.Post && path == "/"
                        }
                        session<ClientSession> {
                            onFail { respond(HttpStatusCode.BadRequest) }
                        }
                    }

                    val responseAcquireSession = client.post("/acquire_session")
                    responseAcquireSession shouldHaveStatus HttpStatusCode.OK
                    val cookie = responseAcquireSession.setCookie().firstOrNull() ?: fail("no set-cookie")
                    val responseRoot1 = client.post("/") {
                        cookie(cookie.name, cookie.value)
                    }
                    responseRoot1 shouldHaveStatus HttpStatusCode.BadRequest
                    val csrfToken = responseRoot1.headers[X_CSRF_TOKEN] ?: fail("no csrf token")
                    val responseRoot2 = client.post("/") {
                        cookie(cookie.name, cookie.value)
                        headers.append(X_CSRF_TOKEN, csrfToken)
                    }
                    responseRoot2 shouldHaveStatus HttpStatusCode.OK
                }
            }
        }
    }

    data class ClientSession(
        override val representation: String
    ) : CsrfTokenBoundClient

}
