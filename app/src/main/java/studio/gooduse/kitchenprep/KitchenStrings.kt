package studio.gooduse.kitchenprep

import android.content.Context
import org.json.JSONObject

data class LocaleChoice(val tag: String, val fallbackName: String)

class KitchenStrings private constructor(
    private val dictionaries: Map<String, Dictionary>,
) {
    data class Dictionary(
        val name: String,
        val direction: String,
        val strings: Map<String, String>,
    )

    fun text(languageTag: String, key: String, fallback: String): String {
        val dictionary = resolve(languageTag) ?: dictionaries["en"]
        return dictionary?.strings?.get(key).orEmpty().ifBlank { fallback }
    }

    fun languageName(languageTag: String, fallback: String): String =
        resolve(languageTag)?.name?.ifBlank { fallback } ?: fallback

    fun isRtl(languageTag: String): Boolean =
        resolve(languageTag)?.direction == "rtl" ||
            languageTag.startsWith("ar", ignoreCase = true) ||
            languageTag.startsWith("he", ignoreCase = true)

    private fun resolve(languageTag: String): Dictionary? {
        val normalized = languageTag.replace('_', '-')
        val language = normalized.substringBefore('-')
        val candidates = buildList {
            add(normalized)
            add(normalized.lowercase())
            add(language)
            when {
                normalized.startsWith("zh-Hant", true) || normalized.startsWith("zh-TW", true) ||
                    normalized.startsWith("zh-HK", true) -> {
                    add("zh-Hant"); add("zhHant"); add("zh_TW"); add("zh-TW"); add("zhTW")
                }
                normalized.startsWith("zh", true) -> {
                    add("zh-Hans"); add("zhHans"); add("zh_CN"); add("zh-CN"); add("zhCN"); add("zh")
                }
                normalized.startsWith("fil", true) -> add("tl")
                normalized.startsWith("nb", true) -> add("no")
            }
        }
        return candidates.firstNotNullOfOrNull { candidate ->
            dictionaries[candidate] ?: dictionaries.entries.firstOrNull {
                it.key.equals(candidate, ignoreCase = true)
            }?.value
        }
    }

    companion object {
        val supportedLocales = listOf(
            LocaleChoice("en", "English"),
            LocaleChoice("es", "Español"),
            LocaleChoice("pt", "Português"),
            LocaleChoice("fr", "Français"),
            LocaleChoice("de", "Deutsch"),
            LocaleChoice("it", "Italiano"),
            LocaleChoice("nl", "Nederlands"),
            LocaleChoice("pl", "Polski"),
            LocaleChoice("cs", "Čeština"),
            LocaleChoice("ro", "Română"),
            LocaleChoice("hu", "Magyar"),
            LocaleChoice("sv", "Svenska"),
            LocaleChoice("da", "Dansk"),
            LocaleChoice("nb", "Norsk bokmål"),
            LocaleChoice("fi", "Suomi"),
            LocaleChoice("tr", "Türkçe"),
            LocaleChoice("ar", "العربية"),
            LocaleChoice("he", "עברית"),
            LocaleChoice("hi", "हिन्दी"),
            LocaleChoice("bn", "বাংলা"),
            LocaleChoice("pa", "ਪੰਜਾਬੀ"),
            LocaleChoice("id", "Bahasa Indonesia"),
            LocaleChoice("ms", "Bahasa Melayu"),
            LocaleChoice("fil", "Filipino"),
            LocaleChoice("vi", "Tiếng Việt"),
            LocaleChoice("th", "ไทย"),
            LocaleChoice("ja", "日本語"),
            LocaleChoice("ko", "한국어"),
            LocaleChoice("zh-Hans", "简体中文"),
            LocaleChoice("zh-Hant", "繁體中文"),
            LocaleChoice("ru", "Русский"),
        )

        fun load(context: Context): KitchenStrings {
            val raw = runCatching {
                context.assets.open("i18n.json").bufferedReader().use { it.readText() }
            }.getOrElse { """{"en":{"name":"English","dir":"ltr","s":{}}}""" }
            val root = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            val dictionaries = linkedMapOf<String, Dictionary>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val code = keys.next()
                val value = root.optJSONObject(code) ?: continue
                val entries = linkedMapOf<String, String>()
                val strings = value.optJSONObject("s") ?: JSONObject()
                val stringKeys = strings.keys()
                while (stringKeys.hasNext()) {
                    val key = stringKeys.next()
                    entries[key] = strings.optString(key)
                }
                dictionaries[code] = Dictionary(
                    name = value.optString("name", code),
                    direction = value.optString("dir", "ltr"),
                    strings = entries,
                )
            }
            if ("en" !in dictionaries) {
                dictionaries["en"] = Dictionary("English", "ltr", emptyMap())
            }
            return KitchenStrings(dictionaries)
        }
    }
}
