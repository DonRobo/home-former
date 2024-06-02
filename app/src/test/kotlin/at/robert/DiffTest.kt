package at.robert

import kotlin.test.Test
import kotlin.test.assertEquals

class DiffTest {

    @Test
    fun `can diff strings`() {
        val old = "old"
        val new = "new"

        assertEquals(
            Diff(listOf(Change(emptyList(), old, new))),
            diff(old, new)
        )
    }

    @Test
    fun `can diff ints`() {
        val old = 1
        val new = 2

        assertEquals(
            Diff(listOf(Change(emptyList(), old, new))),
            diff(old, new)
        )
    }

    @Test
    fun `can diff simple data class`() {
        data class MyData(
            val value1: String,
            val value2: String,
            val value3: String,
        )

        val old = MyData(
            value1 = "old1",
            value2 = "old2",
            value3 = "old3",
        )
        val new = MyData(
            value1 = "new1",
            value2 = "new2",
            value3 = "old3",
        )

        assertEquals(
            Diff(
                listOf(
                    Change(listOf("value1"), "old1", "new1"),
                    Change(listOf("value2"), "old2", "new2"),
                )
            ),
            diff(old, new)
        )
    }

    @Test
    fun `can diff lists`() {
        val old = listOf("old0", "old1", "old2", "old3", "old4")
        val new = listOf("old0", "new1", "old2", "old4")

        assertEquals(
            Diff(
                listOf(
                    Change(listOf("[1]"), "old1", "new1"),
                    Change(listOf("[3]"), "old3", "old4"),
                    Change(listOf("[4]"), "old4", null),
                )
            ),
            diff(old, new)
        )
    }

    @Test
    fun canDiffComplexStructure() {
        data class MyInnerData(
            val value1: String,
            val value2: Int,
            val value3: Map<String, Set<Int>>,
        )

        data class MyOuterData(
            val value1: MyInnerData,
            val value2: List<MyInnerData>,
            val value3: Map<String, MyInnerData>,
        )

        val old = MyOuterData(
            value1 = MyInnerData(
                value1 = "old1",
                value2 = 1,
                value3 = mapOf(
                    "old1" to setOf(1, 2, 3),
                    "old2" to setOf(4, 5, 6),
                ),
            ),
            value2 = listOf(
                MyInnerData(
                    value1 = "old1",
                    value2 = 1,
                    value3 = mapOf(
                        "old1" to setOf(1, 2, 3),
                        "old2" to setOf(4, 5, 6),
                    ),
                ),
                MyInnerData(
                    value1 = "old2",
                    value2 = 2,
                    value3 = mapOf(
                        "old1" to setOf(1, 2, 3),
                        "old2" to setOf(4, 5, 6),
                    ),
                ),
            ),
            value3 = mapOf(
                "old1" to MyInnerData(
                    value1 = "old1",
                    value2 = 1,
                    value3 = mapOf(
                        "old1" to setOf(1, 2, 3),
                        "old2" to setOf(4, 5, 6),
                    ),
                ),
                "old2" to MyInnerData(
                    value1 = "old2",
                    value2 = 2,
                    value3 = mapOf(
                        "old1" to setOf(1, 2, 3),
                        "old2" to setOf(4, 5, 6),
                    ),
                ),
            ),
        )

        val new = MyOuterData(
            value1 = MyInnerData(
                value1 = "new1",
                value2 = 1,
                value3 = mapOf(
                    "old1" to setOf(1, 2, 3),
                    "old2" to setOf(4, 5, 6),
                ),
            ),
            value2 = listOf(
                MyInnerData(
                    value1 = "old1",
                    value2 = 1,
                    value3 = mapOf(
                        "old1" to setOf(1, 2, 3),
                        "old2" to setOf(4, 5, 6),
                    ),
                ),
                MyInnerData(
                    value1 = "new2",
                    value2 = 2,
                    value3 = mapOf(
                        "old1" to setOf(1, 2, 3),
                        "old2" to setOf(4, 5, 6),
                    ),
                ),
            ),
            value3 = mapOf(
                "old1" to MyInnerData(
                    value1 = "old1",
                    value2 = 1,
                    value3 = mapOf(
                        "old1" to setOf(1, 2, 3),
                        "old2" to setOf(4, 5, 6),
                    ),
                ),
                "old2" to MyInnerData(
                    value1 = "old2",
                    value2 = 2,
                    value3 = mapOf(
                        "old1" to setOf(1, 2, 3),
                        "old2" to setOf(4, 5, 6),
                    ),
                ),
            ),
        )

        assertEquals(
            Diff(
                listOf(
                    Change(listOf("value1", "value1"), "old1", "new1"),
                    Change(listOf("value2", "[1]", "value1"), "old2", "new2"),
                )
            ),
            diff(old, new)
        )
    }
}
