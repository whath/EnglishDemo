package com.englishcoach60.domain.language

/** Returns true when this query contains a CJK unified ideograph. */
fun String.containsHanCharacters(): Boolean = any { character ->
    character.code in 0x3400..0x4DBF ||
        character.code in 0x4E00..0x9FFF ||
        character.code in 0xF900..0xFAFF
}
