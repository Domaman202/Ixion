import com.kingmang.ixion.Ixion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals

class ExamplesTest {
    @Test
    fun adt() {
        ixAssert(
            "adt.ix", """
                value 10 is integer
                value 10.0 is float
                
                """.trimIndent()
        )
    }

    @Test
    fun generics() {
        ixAssert(
            "generics.ix", """
                Hello
                10
                
                """.trimIndent()
        )
    }

    @Test
    fun loops() {
        ixAssert(
            "loops.ix", """
                1
                2
                3
                4
                5
                ----
                i is 10
                i is 11
                i is 12
                i is 13
                i is 14
                i is 15
                i is 16
                i is 17
                i is 18
                i is 19
                
                """.trimIndent()
        )
    }

    @Test
    fun simple_list() {
        ixAssert(
            "simple_list.ix", """
                [1, 2, 3]
                [20]

                """.trimIndent()
        )
    }

    @Test
    fun struct() {
        ixAssert(
            "struct.ix", """
                value{left{first}, right{second}}
                
                """.trimIndent()
        )
    }

    fun ixAssert(runPath: String?, resPath: String?) {
        val api = Ixion()
        assertDoesNotThrow {
            assertEquals(
                resPath,
                api.getCompiledProgramOutput("/src/test/resources/$runPath")
            )
        }
    }
}