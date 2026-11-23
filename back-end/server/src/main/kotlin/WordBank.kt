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
    private val availableWords = words.toMutableSet()
    private var currentWord: String? = null

    // functions

    /*
    1. populate the list
    2. give the user the current word
    3. update the list
    4. reset the word list at the end of the game
     */

    fun getCurrentWord(): String? {
        return currentWord
    }

    fun loadJson(json: String) {
        val file = Json.decodeFromString<WordFile>(json)
        availableWords.addAll(file.words)
    }

    // fixed null error, the function will return null, if currentWord == null
    fun chooseWord(): String? {
        currentWord = availableWords.randomOrNull()
        return currentWord
    }

    // updateList now returns string for read only, so no one else can edit the list
    fun updateList(): Set<String> {
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