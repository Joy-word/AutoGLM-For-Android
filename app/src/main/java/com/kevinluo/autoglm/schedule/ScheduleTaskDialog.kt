package com.kevinluo.autoglm.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.kevinluo.autoglm.R
import com.kevinluo.autoglm.util.showWithPrimaryButtons
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Dialog for creating a new scheduled task.
 *
 * Allows the user to:
 * - View the task description
 * - Choose between specific time or delayed execution
 * - Select date and time (for specific time mode)
 * - Enter delay hours and minutes (for delay mode)
 * - Select repeat type (once, daily, weekdays, weekly)
 */
class ScheduleTaskDialog(
    private val context: Context,
    private val taskDescription: String,
    private val onScheduled: (ScheduledTask) -> Unit
) {
    private val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_schedule_task, null)
    
    private val tvTaskPreview: TextView = dialogView.findViewById(R.id.tvTaskPreview)
    private val rgTimeMode: RadioGroup = dialogView.findViewById(R.id.rgTimeMode)
    private val layoutSpecificTime: View = dialogView.findViewById(R.id.layoutSpecificTime)
    private val layoutDelayTime: View = dialogView.findViewById(R.id.layoutDelayTime)
    private val cardSelectDateTime: MaterialCardView = dialogView.findViewById(R.id.cardSelectDateTime)
    private val tvSelectedDateTime: TextView = dialogView.findViewById(R.id.tvSelectedDateTime)
    private val etDelayHours: TextInputEditText = dialogView.findViewById(R.id.etDelayHours)
    private val etDelayMinutes: TextInputEditText = dialogView.findViewById(R.id.etDelayMinutes)
    private val spinnerRepeatType: AutoCompleteTextView = dialogView.findViewById(R.id.spinnerRepeatType)

    private var selectedCalendar = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    init {
        setupViews()
    }

    private fun setupViews() {
        // Set task description preview
        tvTaskPreview.text = taskDescription

        // Setup time mode radio group
        rgTimeMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSpecificTime -> {
                    layoutSpecificTime.visibility = View.VISIBLE
                    layoutDelayTime.visibility = View.GONE
                }
                R.id.rbDelayTime -> {
                    layoutSpecificTime.visibility = View.GONE
                    layoutDelayTime.visibility = View.VISIBLE
                }
            }
        }

        // Setup date/time picker
        updateDateTimeDisplay()
        cardSelectDateTime.setOnClickListener {
            showDateTimePicker()
        }

        // Setup repeat type spinner
        val repeatTypes = listOf(
            context.getString(R.string.schedule_repeat_once),
            context.getString(R.string.schedule_repeat_daily),
            context.getString(R.string.schedule_repeat_weekdays),
            context.getString(R.string.schedule_repeat_weekly)
        )
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, repeatTypes)
        spinnerRepeatType.setAdapter(adapter)
        spinnerRepeatType.setText(repeatTypes[0], false)
    }

    fun show() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.schedule_task_title)
            .setView(dialogView)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                saveScheduledTask()
            }
            .setNegativeButton(R.string.cancel, null)
            .showWithPrimaryButtons()
    }

    private fun showDateTimePicker() {
        // First show date picker
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                // Then show time picker
                showTimePicker()
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Don't allow selecting past dates
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedCalendar.set(Calendar.MINUTE, minute)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                updateDateTimeDisplay()
            },
            selectedCalendar.get(Calendar.HOUR_OF_DAY),
            selectedCalendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateDateTimeDisplay() {
        val now = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val dateStr = when {
            isSameDay(selectedCalendar, now) -> context.getString(R.string.schedule_today)
            isTomorrow(selectedCalendar, now) -> context.getString(R.string.schedule_tomorrow)
            else -> dateFormat.format(selectedCalendar.time)
        }
        
        val timeStr = timeFormat.format(selectedCalendar.time)
        tvSelectedDateTime.text = "$dateStr $timeStr"
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isTomorrow(cal1: Calendar, cal2: Calendar): Boolean {
        val tomorrow = cal2.clone() as Calendar
        tomorrow.add(Calendar.DAY_OF_MONTH, 1)
        return isSameDay(cal1, tomorrow)
    }

    private fun saveScheduledTask() {
        // Calculate execution time
        val executionTime = if (rgTimeMode.checkedRadioButtonId == R.id.rbSpecificTime) {
            // Specific time mode
            if (selectedCalendar.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(context, R.string.schedule_time_past, Toast.LENGTH_SHORT).show()
                return
            }
            selectedCalendar.timeInMillis
        } else {
            // Delay mode
            val hours = etDelayHours.text.toString().toIntOrNull() ?: 0
            val minutes = etDelayMinutes.text.toString().toIntOrNull() ?: 0
            
            if (hours == 0 && minutes == 0) {
                Toast.makeText(context, R.string.schedule_time_past, Toast.LENGTH_SHORT).show()
                return
            }
            
            System.currentTimeMillis() + (hours * 3600000L) + (minutes * 60000L)
        }

        // Get repeat type
        val repeatTypeIndex = when (spinnerRepeatType.text.toString()) {
            context.getString(R.string.schedule_repeat_once) -> 0
            context.getString(R.string.schedule_repeat_daily) -> 1
            context.getString(R.string.schedule_repeat_weekdays) -> 2
            context.getString(R.string.schedule_repeat_weekly) -> 3
            else -> 0
        }
        
        val repeatType = when (repeatTypeIndex) {
            0 -> RepeatType.ONCE
            1 -> RepeatType.DAILY
            2 -> RepeatType.WEEKDAYS
            3 -> RepeatType.WEEKLY
            else -> RepeatType.ONCE
        }

        // Create scheduled task
        val taskManager = ScheduledTaskManager.getInstance(context)
        val task = ScheduledTask(
            id = taskManager.generateTaskId(),
            taskDescription = taskDescription,
            scheduledTimeMillis = executionTime,
            repeatType = repeatType,
            isEnabled = true
        )

        onScheduled(task)
    }
}
