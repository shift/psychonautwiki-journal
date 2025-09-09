package com.isaakhanimann.journal.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AdministrationRoute {
    ORAL,
    SUBLINGUAL,
    BUCCAL,
    INSUFFLATED,
    RECTAL,
    TRANSDERMAL,
    SUBCUTANEOUS,
    INTRAMUSCULAR,
    INTRAVENOUS,
    SMOKED,
    INHALED;
    
    val displayName: String
        get() = when (this) {
            ORAL -> "Oral"
            SUBLINGUAL -> "Sublingual"
            BUCCAL -> "Buccal"
            INSUFFLATED -> "Insufflated"
            RECTAL -> "Rectal"
            TRANSDERMAL -> "Transdermal"
            SUBCUTANEOUS -> "Subcutaneous"
            INTRAMUSCULAR -> "Intramuscular"
            INTRAVENOUS -> "Intravenous"
            SMOKED -> "Smoked"
            INHALED -> "Inhaled"
        }
}

@Serializable
enum class StomachFullness {
    EMPTY,
    QUARTER_FULL,
    HALF_FULL,
    FULL,
    VERY_FULL;
    
    val displayName: String
        get() = when (this) {
            EMPTY -> "Empty"
            QUARTER_FULL -> "Quarter Full"
            HALF_FULL -> "Half Full"
            FULL -> "Full"
            VERY_FULL -> "Very Full"
        }
}

@Serializable
enum class ShulginRatingOption {
    MINUS,
    PLUS_MINUS,
    PLUS,
    TWO_PLUS,
    THREE_PLUS,
    FOUR_PLUS;
    
    val displayName: String
        get() = when (this) {
            MINUS -> "−"
            PLUS_MINUS -> "±"
            PLUS -> "+"
            TWO_PLUS -> "++"
            THREE_PLUS -> "+++"
            FOUR_PLUS -> "++++"
        }
    
    val description: String
        get() = when (this) {
            MINUS -> "No effect"
            PLUS_MINUS -> "Threshold effect"
            PLUS -> "Light effect"
            TWO_PLUS -> "Moderate effect"
            THREE_PLUS -> "Strong effect"
            FOUR_PLUS -> "Very strong effect"
        }
}

@Serializable
enum class AdaptiveColor {
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    MINT,
    TEAL,
    CYAN,
    BLUE,
    INDIGO,
    PURPLE,
    PINK,
    BROWN,
    WHITE,
    GRAY,
    BLACK;
    
    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.titlecase() }
}