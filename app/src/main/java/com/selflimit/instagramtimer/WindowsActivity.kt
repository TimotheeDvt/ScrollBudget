package com.selflimit.instagramtimer

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.selflimit.instagramtimer.data.TimeWindow
import com.selflimit.instagramtimer.data.WindowRepository
import com.selflimit.instagramtimer.databinding.ActivityWindowsBinding
import com.selflimit.instagramtimer.databinding.DialogEditWindowBinding
import com.selflimit.instagramtimer.databinding.ItemWindowEditBinding
import com.selflimit.instagramtimer.util.TimeSlots
import java.util.UUID

class WindowsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWindowsBinding
    private lateinit var windowRepository: WindowRepository
    private val windows = mutableListOf<TimeWindow>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWindowsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        windowRepository = WindowRepository(this)
        windows.addAll(windowRepository.getWindows().sortedBy { it.startMinute })
        renderList()

        binding.addWindowButton.setOnClickListener { showEditDialog(null) }
    }

    private fun renderList() {
        binding.windowsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (window in windows) {
            val row = ItemWindowEditBinding.inflate(inflater, binding.windowsContainer, false)
            row.windowLabel.text = getString(
                R.string.edit_row_format,
                TimeSlots.label(window.startMinute),
                TimeSlots.label(window.endMinute),
                window.capMinutes
            )
            row.editButton.setOnClickListener { showEditDialog(window) }
            row.deleteButton.setOnClickListener { confirmDelete(window) }
            binding.windowsContainer.addView(row.root)
        }
    }

    private fun showEditDialog(existing: TimeWindow?) {
        val dialogBinding = DialogEditWindowBinding.inflate(layoutInflater)

        val startLabels = TimeSlots.startOptions.map { TimeSlots.label(it) }
        val startAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, startLabels)
        startAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.startTimeSpinner.adapter = startAdapter

        val endLabels = TimeSlots.endOptions.map { TimeSlots.label(it) }
        val endAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, endLabels)
        endAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.endTimeSpinner.adapter = endAdapter

        if (existing != null) {
            dialogBinding.startTimeSpinner.setSelection(TimeSlots.startOptions.indexOf(existing.startMinute))
            dialogBinding.endTimeSpinner.setSelection(TimeSlots.endOptions.indexOf(existing.endMinute))
            dialogBinding.capInput.setText(existing.capMinutes.toString())
        } else {
            dialogBinding.capInput.setText(DEFAULT_NEW_CAP_MINUTES.toString())
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) R.string.add_window_dialog_title else R.string.edit_window_dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val startMinute = TimeSlots.startOptions[dialogBinding.startTimeSpinner.selectedItemPosition]
                val endMinute = TimeSlots.endOptions[dialogBinding.endTimeSpinner.selectedItemPosition]
                val capMinutes = dialogBinding.capInput.text.toString().toIntOrNull()

                val errorRes = validate(startMinute, endMinute, capMinutes, excludingId = existing?.id)
                if (errorRes != null) {
                    Toast.makeText(this, errorRes, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newWindow = TimeWindow(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    startMinute = startMinute,
                    endMinute = endMinute,
                    capMinutes = capMinutes!!
                )
                existing?.let { windows.remove(it) }
                windows.add(newWindow)
                windows.sortBy { it.startMinute }
                windowRepository.saveWindows(windows)
                renderList()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validate(start: Int, end: Int, cap: Int?, excludingId: String?): Int? {
        if (cap == null || cap <= 0) return R.string.error_invalid_cap
        if (end <= start) return R.string.error_end_before_start
        val overlaps = windows.any { it.id != excludingId && start < it.endMinute && it.startMinute < end }
        if (overlaps) return R.string.error_overlap
        return null
    }

    private fun confirmDelete(window: TimeWindow) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_window_title)
            .setMessage(
                getString(
                    R.string.delete_window_message,
                    TimeSlots.label(window.startMinute),
                    TimeSlots.label(window.endMinute)
                )
            )
            .setPositiveButton(R.string.delete) { _, _ ->
                windows.remove(window)
                windowRepository.saveWindows(windows)
                renderList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        private const val DEFAULT_NEW_CAP_MINUTES = 20
    }
}
