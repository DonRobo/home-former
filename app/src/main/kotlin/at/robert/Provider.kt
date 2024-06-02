package at.robert

interface Provider<T> {
    fun currentState(): T
    fun applyDiff(diff: Diff)
}
