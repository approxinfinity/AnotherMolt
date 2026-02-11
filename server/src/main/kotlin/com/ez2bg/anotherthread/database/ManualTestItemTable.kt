package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

/**
 * Table for tracking manual testing items.
 * Admin users can mark features as tested to track QA coverage.
 */
object ManualTestItemTable : Table("manual_test_items") {
    val id = varchar("id", 36)
    val featureName = text("feature_name")
    val description = text("description")
    val category = varchar("category", 50)  // e.g., "combat", "faction", "ui", "navigation"
    val commitHash = varchar("commit_hash", 40).nullable()  // Git commit that added this feature
    val addedAt = long("added_at")  // Timestamp when the test item was created
    val testedAt = long("tested_at").nullable()  // Timestamp when marked as tested (null if untested)
    val testedByUserId = varchar("tested_by_user_id", 36).nullable()
    val testedByUserName = text("tested_by_user_name").nullable()
    val notes = text("notes").nullable()  // Optional notes from tester

    override val primaryKey = PrimaryKey(id)
}
