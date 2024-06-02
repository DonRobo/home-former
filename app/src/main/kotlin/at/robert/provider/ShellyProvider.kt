package at.robert.provider

import at.robert.Diff
import at.robert.shelly.ShellyClient

data class ShellyState(
    val name: String,
)

class ShellyProvider : Provider<ShellyState> {
    override var config: Any
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun currentState(): ShellyState {
        val shellyClient = ShellyClient("TODO")
        TODO("Not yet implemented")
    }

    override fun applyDiff(diff: Diff) {
        TODO("Not yet implemented")
    }
}
