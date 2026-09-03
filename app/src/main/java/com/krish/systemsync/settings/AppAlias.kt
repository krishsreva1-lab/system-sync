package com.krish.systemsync.settings

import com.krish.systemsync.R

enum class AppAlias(
    val labelRes: Int,
    val iconRes: Int,
    val className: String
) {
    DEFAULT(
        R.string.app_name,
        R.mipmap.ic_launcher,
        "com.krish.systemsync.LauncherDefault"
    ),
    CALCULATOR(
        R.string.alias_calculator,
        R.drawable.ic_alias_calculator,
        "com.krish.systemsync.AliasCalculator"
    ),
    WEATHER(
        R.string.alias_weather,
        R.drawable.ic_alias_weather,
        "com.krish.systemsync.AliasWeather"
    ),
    COMPASS(
        R.string.alias_compass,
        R.drawable.ic_alias_compass,
        "com.krish.systemsync.AliasCompass"
    ),
    NOTES(
        R.string.alias_notes,
        R.drawable.ic_alias_notes,
        "com.krish.systemsync.AliasNotes"
    ),
    SETTINGS(
        R.string.alias_settings,
        R.drawable.ic_alias_settings,
        "com.krish.systemsync.AliasSettings"
    );

    companion object {
        fun fromClassName(className: String): AppAlias {
            return values().find { it.className == className } ?: DEFAULT
        }
    }
}
