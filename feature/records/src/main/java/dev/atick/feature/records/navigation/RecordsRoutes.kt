package dev.atick.feature.records.navigation

import kotlinx.serialization.Serializable

@Serializable object RecordsRoot
@Serializable object Records                     // list screen
@Serializable data class RecordDetail(val recordId: String)