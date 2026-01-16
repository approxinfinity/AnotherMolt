package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object FeatureCategoryTable : Table("feature_category") {
    val id = varchar("id", 36)
    val name = text("name")
    val description = text("description")

    override val primaryKey = PrimaryKey(id)
}
