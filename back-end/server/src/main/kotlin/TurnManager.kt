package it.inkorporate

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class WordFile( // this file is used to parse JSON file of words
    val words: List<String> = emptyList()
)

class WordBank(
    words: List<String> = emptyList<String>()
    // this parameter has no val/var, is only used for initialization
){
    // variables
    private val availableWords = mutableSetOf<String>()
    private var currentWord: String? = null // start with an empty word

    // functions

    /*
    1. populate the list
    2. give the user the current word
    3. update used words
    4. update available words
    5. clear the word list at the end of the game
     */

    fun loadJson(json: String) {
        val file = Json.decodeFromString<WordFile>(json)
        availableWords.addAll(file.words)
    }

    fun getCurrentWord(): String? {
        return currentWord
    }

}