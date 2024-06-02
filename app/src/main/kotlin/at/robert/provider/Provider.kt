package at.robert.provider

import at.robert.Diff

interface Provider<T> {
    fun currentState(): T
    fun applyDiff(diff: Diff)
}
