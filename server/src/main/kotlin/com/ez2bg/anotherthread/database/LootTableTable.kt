package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object LootTableTable : Table("loot_table") {
    val id = varchar("id", 36)
    val name = text("name")
    val entries = text("entries")  // JSON: [{"itemId": "x", "chance": 0.25, "minQty": 1, "maxQty": 1}]

    override val primaryKey = PrimaryKey(id)
}
