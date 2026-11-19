package it.inkorporate

import kotlinx.serialization.Serializable
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
    private var currentWord: String? = null

    // functions

    /*
    1. populate the list
    2. give the user the current word
    3. update the list
    5. reset the word list at the end of the game
     */

    fun getCurrentWord(): String? {
        return currentWord
    }

    fun loadJson(json: String) {
        val file = Json.decodeFromString<WordFile>(json)
        availableWords.addAll(file.words)
    }

    fun chooseWord(): String? {
        currentWord = availableWords.random()
        return currentWord
    }

    fun updateList(): MutableSet<String> {
        // if there is a current word, and it is still available, remove it
        currentWord?.let {
            if (it in availableWords) {
                availableWords.remove(it)
            }
        }
        return availableWords
    }

    fun resetList(){
        return availableWords.clear()
    }

}