package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object FeatureTable : Table("feature") {
    val id = varchar("id", 36)
    val name = text("name")
    val featureCategoryId = varchar("feature_category_id", 36).nullable()
    val description = text("description")
    val data = text("data")

    override val primaryKey = PrimaryKey(id)
}
