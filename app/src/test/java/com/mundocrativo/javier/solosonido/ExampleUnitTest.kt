package com.mundocrativo.javier.solosonido

import com.mundocrativo.javier.solosonido.util.Util
import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun checkMd5(){
        val result = Util.hexMd5Checksum("https://youtu.be/9oTHQ6VFSPg")
        val elValor = "a5f456a3705dca687da9ac6715690b62"
        Assert.assertTrue(result.contentEquals(elValor) )
    }
}