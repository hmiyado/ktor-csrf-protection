package com.github.hmiyado.csrfprotection

import io.ktor.application.ApplicationCall
import kotlin.properties.Delegates

class CsrfContext(
    val call: ApplicationCall,
) {
    var isValid: Boolean? by Delegates.vetoable(null) { _, old, _ ->
        require(old == null) { "isValid can be only assigned once" }
        true
    }
}
