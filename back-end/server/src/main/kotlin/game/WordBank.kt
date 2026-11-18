
enum class Difficulty {EASY, MEDIUM, HARD } // defines the 3 types, keys for easy look up


@Serializable // mirriors the json structure, Only used for parsing
private data class WordFile(
    val easy: List<String> = emptyList(),
    val medium: List<String> = emptyList(),
    val hard: List<String> = emptyList()
)

