package com.kevinluo.autoglm.schedule

/**
 * Represents a scheduled task that will be executed at a specific time.
 *
 * @property id Unique identifier for the scheduled task
 * @property taskDescription The description of the task to execute
 * @property scheduledTimeMillis The absolute timestamp (in milliseconds) for the first execution
 * @property repeatType How the task should repeat
 * @property isEnabled Whether the task is currently enabled
 * @property createdAt Timestamp when the task was created
 * @property lastExecutedAt Timestamp when the task was last executed (null if never executed)
 */
data class ScheduledTask(
    val id: String,
    val taskDescription: String,
    val scheduledTimeMillis: Long,
    val repeatType: RepeatType,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastExecutedAt: Long? = null,
)

/**
 * Enum representing how a scheduled task should repeat.
 */
enum class RepeatType {
    /** Execute only once */
    ONCE,

    /** Execute every day at the same time */
    DAILY,

    /** Execute on weekdays (Monday-Friday) */
    WEEKDAYS,

    /** Execute every week on the same day */
    WEEKLY,
}
