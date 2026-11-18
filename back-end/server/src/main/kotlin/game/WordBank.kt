package it.inkorporate.game


enum class Difficulty {EASY, MEDIUM, HARD } // defines the 3 types, keys for easy look up


// need to add serializable import and dependency
//@Serializable // mirriors the json structure
private data class WordFile(
    // this file is used to parse the json file of the 3 word lists
    val easy: List<String> = emptyList(),
    val medium: List<String> = emptyList(),
    val hard: List<String> = emptyList()
)

class WordBank( // creates an empty wordBank
    easyWords: List<String> = emptyList(),
    mediumWords: List<String> = emptyList(),
    hardWords: List<String> = emptyList()
    // these parameters have no val/var, are only used for initialization
) {
    /*
    create a map that pair the key (word difficulty) to the value (word difficulty list)
    so an example would be,
    key (Difficulty.EASY), value (["dog", "cat", etc])
     */
    private val wordsByDifficulty = mapOf(
        Difficulty.EASY to easyWords.toMutableList(),
        Difficulty.MEDIUM to mediumWords.toMutableList(),
        Difficulty.HARD to hardWords.toMutableList()
    )

    private val usedWords = mutableSetOf<String>() // only WordBank can change this

    private currentWord: String? = null // ? allows for string and nullable types

    // functions

    // getter
    fun getCurrentWord(): String {
        return currentWord
    }

    // returns every word, across all 3 difficulties that has not been used yet
    fun updateAvailableWords(): List<String> {
        return wordsByDifficulty.values // grab the three list
            .flatten() // merge all 3 lists into one
            .filter { currentWord !in usedWords } // filter the list if currentWord is not in usedWords list
    }

    fun resetUsedWords() {
        usedWords.clear()
        currentWord = null
    }

}