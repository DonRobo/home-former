package at.robert.hf.htmx

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.html.*

val hxObjectMapper = jacksonObjectMapper()
fun Tag.hxPost(hxCtx: HtmxContext, vararg params: Pair<String, Any>) {
    attributes["hx-post"] = hxCtx.route
    attributes["hx-target"] = "#${hxCtx.component}"
    attributes["hx-swap"] = "innerHTML"
    attributes["hx-vals"] = params.toMap().let { hxObjectMapper.writeValueAsString(it) }
}

fun FlowContent.hxActionForm(
    hxCtx: HtmxContext,
    actionLabel: String,
    block: FORM.() -> Unit
) {
    form {
        attributes["hx-post"] = hxCtx.route
        attributes["hx-target"] = "#${hxCtx.component}"
        attributes["hx-swap"] = "innerHTML"

        block()
        input(type = InputType.submit) {
            value = actionLabel
        }
    }
}

