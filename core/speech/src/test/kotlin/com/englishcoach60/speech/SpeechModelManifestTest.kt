package com.englishcoach60.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechModelManifestTest {
    @Test
    fun manifestHasUniqueFileNamesAndValidChecksums() {
        val files = ParakeetEnglishModel.files

        assertEquals(files.size, files.map { it.name }.distinct().size)
        assertTrue(files.all { it.byteCount > 0 })
        assertTrue(files.all { it.sha256.matches(Regex("[0-9a-f]{64}")) })
    }

    @Test
    fun totalSizeMatchesManifest() {
        assertEquals(
            ParakeetEnglishModel.files.sumOf(SpeechModelFile::byteCount),
            ParakeetEnglishModel.totalBytes,
        )
    }
}
