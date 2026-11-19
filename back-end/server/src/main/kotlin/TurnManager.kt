package it.inkorporate

import kotlinx.serialization.Serializable

@Serializable
data class WordFile( // this file is used to parse JSON file of words
    val words: List<String> = emptyList()
)