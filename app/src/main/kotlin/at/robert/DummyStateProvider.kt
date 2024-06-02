package at.robert

data class DummyState(
    val state: String = "state1"
)

class DummyStateProvider : Provider<DummyState> {
    private var currentState = DummyState()
    override fun currentState(): DummyState {
        return currentState
    }

    override fun applyDiff(diff: Diff) {
        diff.changes.forEach { change ->
            if (change.path == listOf("state")) {
                currentState = currentState.copy(state = change.newValue as String)
            } else {
                error("Can't apply diff $diff to ${currentState()}")
            }
        }
    }
}
