package io.pleo.antaeus.rest

import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.http.Handler

enum class Role : RouteRole { ANYONE, USER_READ, USER_WRITE }

object Auth {

    // access-manager is simple
    // when endpoint has io.pleo.antaeus.rest.Role.ANYONE, it will always handle the request
    // when the request has the permitted roles (determined by inspecting the request) it handles the request.
    // else, it sets status 403
    fun accessManager(handler: Handler, ctx: Context, permittedRoles: Set<RouteRole>) {
        when {
            permittedRoles.contains(Role.ANYONE) -> handler.handle(ctx)
            ctx.userRoles.any { it in permittedRoles } -> handler.handle(ctx)
            else -> ctx.status(403).json("Unauthorized")
        }
    }

    // get roles from userRoleMap after extracting username/password from basic-auth header
    private val Context.userRoles: List<Role>
        get() = this.basicAuthCredentials().let { (username, password) ->
            userRoleMap[Pair(username, password)] ?: listOf()
        }

    // we'll store passwords in clear text (and in memory) for this example, but please don't
    private val userRoleMap = hashMapOf(
            Pair("pleo-user", "pleo-user") to listOf(Role.USER_READ),
            Pair("pleo-admin", "pleo-admin") to listOf(Role.USER_READ, Role.USER_WRITE)
    )
}
