package io.github.hmiyado.ktor.csrfprotection

import io.kotest.assertions.fail
import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldNotBe
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.cookiesSession
import io.ktor.server.testing.withTestApplication
import io.ktor.sessions.SessionStorageMemory
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.get
import io.ktor.sessions.header
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import kotlinx.coroutines.runBlocking

private const val CLIENT_SESSION = "client_session"
private const val X_CSRF_TOKEN = "X-CSRF-TOKEN"

class SessionCsrfTest : DescribeSpec() {

    lateinit var testApplicationEngine: TestApplicationEngine

    private val sessionStorage: SessionStorageMemory = SessionStorageMemory()

    override fun beforeTest(testCase: TestCase) {
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

    override fun afterTest(testCase: TestCase, result: TestResult) {
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
                testApplicationEngine.stop(0,0)

                withTestApplication(moduleFunction = {
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
                        cookie<ClientSession>(CLIENT_SESSION, storage = sessionStorage) {}
                        header<CsrfTokenSession>(X_CSRF_TOKEN, storage = sessionStorage) {}
                    }
                    install(Csrf) {
                        requestFilter { httpMethod, path ->
                            httpMethod == HttpMethod.Post && path == "/"
                        }
                        session<ClientSession> {
                            onFail { respond(HttpStatusCode.BadRequest) }
                        }
                    }
                }) {
                    cookiesSession {
                        post("/acquire_session") {}.run {
                            response shouldHaveStatus HttpStatusCode.OK
                            response.cookies[CLIENT_SESSION] shouldNotBe null
                        }
                        post("/") {
                            // no csrf token
                        }.run {
                            response shouldHaveStatus HttpStatusCode.BadRequest
                        }
                        post("/") {
                            // invalid csrf token
                            addHeader(X_CSRF_TOKEN, "invalid_csrf_token")
                        }.run {
                            response shouldHaveStatus HttpStatusCode.BadRequest
                        }
                    }
                }
            }
        }
        describe("valid client session, valid csrf token") {
            it("should be success with valid csrf token session, valid client session") {
                testApplicationEngine.stop(0,0)

                withTestApplication(moduleFunction = {
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
                        cookie<ClientSession>(CLIENT_SESSION, storage = sessionStorage) {}
                        header<CsrfTokenSession>(X_CSRF_TOKEN, storage = sessionStorage) {}
                    }
                    install(Csrf) {
                        requestFilter { httpMethod, path ->
                            httpMethod == HttpMethod.Post && path == "/"
                        }
                        session<ClientSession> {
                            onFail { respond(HttpStatusCode.BadRequest) }
                        }
                    }
                }) {
                    cookiesSession {
                        post("/acquire_session") {}.run {
                            response shouldHaveStatus HttpStatusCode.OK
                            response.cookies[CLIENT_SESSION] shouldNotBe null
                        }
                        val call = post("/") {}
                        call.response shouldHaveStatus HttpStatusCode.BadRequest
                        val csrfToken = call.response.headers[X_CSRF_TOKEN] ?: fail("no csrf token")
                        post("/") {
                            addHeader(X_CSRF_TOKEN, csrfToken)
                        }.run {
                            response shouldHaveStatus HttpStatusCode.OK
                        }
                    }
                }
            }
        }
    }

    data class ClientSession(
        val value: String,
    ) : CsrfTokenBoundClient {
        override val representation: String
            get() = value
    }

}
