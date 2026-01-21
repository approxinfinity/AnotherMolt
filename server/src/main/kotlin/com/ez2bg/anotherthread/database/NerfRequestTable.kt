package com.ez2bg.anotherthread.database

import org.jetbrains.exposed.sql.Table

object NerfRequestTable : Table("nerf_request") {
    val id = varchar("id", 36)
    val abilityId = varchar("ability_id", 36)
    val requestedByUserId = varchar("requested_by_user_id", 36)
    val requestedByUserName = text("requested_by_user_name")
    val reason = text("reason")
    val status = text("status").default("pending")  // pending, approved, rejected, applied
    val suggestedChanges = text("suggested_changes").nullable()  // JSON of suggested ability changes
    val adminNotes = text("admin_notes").nullable()
    val createdAt = long("created_at")
    val resolvedAt = long("resolved_at").nullable()
    val resolvedByUserId = varchar("resolved_by_user_id", 36).nullable()

    override val primaryKey = PrimaryKey(id)
}
