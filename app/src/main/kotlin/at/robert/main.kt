package at.robert

fun main() {
    val stateProvider = DummyStateProvider()
    println(stateProvider.currentState())
    val desiredState = DummyState("state1")
    println("Desired state: $desiredState")
    val changesToApply = diff(stateProvider.currentState(), desiredState)
    if (changesToApply.changes.isNotEmpty()) {
        println("Will apply:")
        changesToApply.changes.forEach {
            println(it)
        }
        stateProvider.applyDiff(changesToApply)
        println("Applied changes. New state: ${stateProvider.currentState()}")
    } else {
        println("No changes to apply")
    }
}
