package com.syathir.roadgpssurvey.export

object ExportFileName {
    fun forSession(name: String, id: Long, format: ExportFormat): String {
        val safeName = name.map { char ->
            if (char.isISOControl() || char in "\\/:*?\"<>|") '_' else char
        }.joinToString("").trim().trim('.').take(80).ifBlank { "Ruas" }
        return "${safeName}_${id}.${format.extension}"
    }
}
