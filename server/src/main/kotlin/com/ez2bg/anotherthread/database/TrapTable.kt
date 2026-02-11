package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

/**
 * Database table for traps.
 * Traps can be placed in locations or on objects (chests, doors) and trigger
 * when players interact without detecting/disarming them first.
 */
object TrapTable : Table("trap") {
    val id = text("id")
    val name = text("name")
    val description = text("description")
    val locationId = text("location_id")

    // Trap type and trigger
    val trapType = text("trap_type")  // "pit", "dart", "poison_needle", "boulder", "alarm", "magic"
    val triggerType = text("trigger_type")  // "movement", "interaction", "door", "chest", "pressure_plate"

    // Detection and disarm
    val detectDifficulty = integer("detect_difficulty").default(2)  // 1-5 scale, DC = 10 + difficulty*2
    val disarmDifficulty = integer("disarm_difficulty").default(2)  // 1-5 scale

    // Effect data
    val effectData = text("effect_data")  // JSON blob with trap-specific parameters

    // State
    val isHidden = bool("is_hidden").default(true)  // Not visible until detected
    val isArmed = bool("is_armed").default(true)  // Can be disarmed
    val resetsAfterRounds = integer("resets_after_rounds").default(0)  // 0 = doesn't reset

    override val primaryKey = PrimaryKey(id)
}
