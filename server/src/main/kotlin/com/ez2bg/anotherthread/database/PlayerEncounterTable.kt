package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object PlayerEncounterTable : Table("player_encounter") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val encounteredUserId = varchar("encountered_user_id", 36)
    val firstEncounteredAt = long("first_encountered_at")
    val lastEncounteredAt = long("last_encountered_at")
    val lastLocationId = varchar("last_location_id", 36).nullable()
    val classification = text("classification").default("neutral") // "friend", "enemy", "neutral"
    val lastKnownDesc = text("last_known_desc").default("")
    val lastKnownImageUrl = text("last_known_image_url").nullable()
    val lastKnownName = text("last_known_name").default("")
    val encounterCount = integer("encounter_count").default(1)

    override val primaryKey = PrimaryKey(id)
}
