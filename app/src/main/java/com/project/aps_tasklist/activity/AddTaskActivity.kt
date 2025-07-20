package com.project.aps_tasklist.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.project.aps_tasklist.R
import com.project.aps_tasklist.model.TaskModel
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import android.content.ContentValues
import android.provider.CalendarContract
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue

class AddTaskActivity : AppCompatActivity() {

    // util: generate random 8-digit alfanumerik untuk Group ID
    private fun randomGroupId(length: Int = 8): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    private lateinit var rgTaskType: RadioGroup
    private lateinit var etTaskID: TextInputEditText
    private lateinit var taskIdLayout: View
    private lateinit var btnAddSubtask: View
    private lateinit var subtaskContainer: LinearLayout
    private lateinit var etDeadlineDate: TextInputEditText
    private lateinit var etDeadlineTime: TextInputEditText
    private lateinit var btnSaveTask: MaterialButton


    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val calendar = Calendar.getInstance()
    private var selectedDeadlineMillis: Long = 0L
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        // Bind views
        rgTaskType       = findViewById(R.id.rgTaskType)
        etTaskID         = findViewById(R.id.etTaskID)
        taskIdLayout     = findViewById(R.id.inputLayoutTaskID)
        btnAddSubtask    = findViewById(R.id.btnAddSubtask)
        subtaskContainer = findViewById(R.id.subtaskContainer)
        etDeadlineDate   = findViewById(R.id.etDeadlineDate)
        etDeadlineTime   = findViewById(R.id.etDeadlineTime)
        btnSaveTask      = findViewById(R.id.btnSaveTask)

        val btnJoinGroup = findViewById<MaterialButton>(R.id.btnJoinGroupTask)

        val etJoinCode = findViewById<TextInputEditText>(R.id.etJoinCode)

        // Toggle Task Type: Individual vs Group
        rgTaskType.setOnCheckedChangeListener { _, checkedId ->
            val isGroup = (checkedId == R.id.rbGroup)
            taskIdLayout.visibility = if (isGroup) View.VISIBLE else View.GONE

            if (isGroup) {
                etTaskID.setText(randomGroupId())  // <-- generate 8-digit ID here
            } else {
                etTaskID.setText("")
            }
            updateAssignSwitchVisibility()
        }

        // Deadline date picker
        etDeadlineDate.setOnClickListener {
            showDatePickerDialog { year, month, day ->
                calendar.set(year, month, day)
                etDeadlineDate.setText(dateFormat.format(calendar.time))
                savePartialDeadline()
            }
        }
        // Deadline time picker
        etDeadlineTime.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                etDeadlineTime.setText(timeFormat.format(calendar.time))
                savePartialDeadline()
            }
        }

        // Add Subtask
        btnAddSubtask.setOnClickListener { addSubtaskView() }

        // Save Task button
        btnSaveTask.setOnClickListener { saveAndReturn() }

        btnJoinGroup.setOnClickListener {
            val groupIdRaw = etJoinCode.text?.toString() ?: ""
            val groupId = groupIdRaw.trim().replace("\\s+".toRegex(), "").uppercase(Locale.getDefault())

            if (groupId.length != 8 || !groupId.matches(Regex("^[A-Z0-9]{8}$"))) {
                Toast.makeText(this, "ID Grup harus tepat 8 karakter A-Z atau 0-9", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            firestore.collection("tasks")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener { query ->
                    if (query.isEmpty) {
                        Toast.makeText(this, "Group ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                    } else {
                        for (doc in query.documents) {
                            firestore.collection("tasks")
                                .document(doc.id)
                                .update("members", FieldValue.arrayUnion(uid))
                        }

                        firestore.collection("groups").document(groupId)
                            .update("members", FieldValue.arrayUnion(uid))
                            .addOnSuccessListener {
                                Toast.makeText(this, "Berhasil join ke grup", Toast.LENGTH_SHORT).show()
                                setResult(RESULT_OK)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Gagal menambahkan ke grup", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal join group", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun savePartialDeadline() {
        selectedDeadlineMillis = calendar.timeInMillis
    }

    private fun saveAndReturn() {
        // 1. Ambil input dasar
        val title    = findViewById<TextInputEditText>(R.id.etTaskTitle).text.toString().trim()
        val desc     = findViewById<TextInputEditText>(R.id.etTaskDesc).text.toString().trim()
        val isGroup  = rgTaskType.checkedRadioButtonId == R.id.rbGroup
        val groupId  = if (isGroup) etTaskID.text.toString().trim().uppercase() else null
        val uid      = auth.currentUser?.uid ?: return
        val nowTs    = Timestamp.now()
        val nowMs    = nowTs.toDate().time

        // 2. Validasi
        if (title.isEmpty()) {
            Toast.makeText(this, "Masukkan judul tugas", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDeadlineMillis <= nowMs) {
            Toast.makeText(this, "Pilih deadline setelah saat ini", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Siapkan parent data
        val parentData = hashMapOf(
            "title"           to title,
            "description"     to desc,
            "type"            to if (isGroup) "group" else "individual",
            "groupId"         to groupId,
            "deadlineMillis"  to selectedDeadlineMillis,
            "createdBy"       to uid,
            "createdAt"          to nowMs,
            "lastInteractedMillis" to nowMs,
            "members"         to listOf(uid)
        )

        firestore.collection("tasks")
            .add(parentData)
            .addOnSuccessListener { taskDoc ->

                val taskId = taskDoc.id

                // 5. Simpan setiap subtask
                for (i in 0 until subtaskContainer.childCount) {
                    val view         = subtaskContainer.getChildAt(i)
                    val nameField    = view.findViewById<EditText>(R.id.etSubtaskName)
                    val dateField    = view.findViewById<EditText>(R.id.etSubtaskDate)
                    val timeField    = view.findViewById<EditText>(R.id.etSubtaskTime)
                    val switchRem    = view.findViewById<Switch>(R.id.switchReminder)
                    val hoursRem     = view.findViewById<EditText>(R.id.etReminderHours).text.toString()
                    val minsRem      = view.findViewById<EditText>(R.id.etReminderMinutes).text.toString()
                    val assignMe     = view.findViewById<Switch>(R.id.switchAssignToMe).isChecked

                    // parse subtask deadline
                    val subCal = Calendar.getInstance().apply {
                        time = dateFormat.parse(dateField.text.toString()) ?: Date()
                        set(Calendar.HOUR_OF_DAY,
                            timeFormat.parse(timeField.text.toString())?.hours ?: 0)
                        set(Calendar.MINUTE,
                            timeFormat.parse(timeField.text.toString())?.minutes ?: 0)
                    }
                    val subDeadline = subCal.timeInMillis

                    // reminder offset
                    val offsetMs = if (switchRem.isChecked) {
                        (hoursRem.toLongOrNull() ?: 0L)*3_600_000 +
                                (minsRem .toLongOrNull() ?: 0L)*   60_000
                    } else 0L

                    val subData = hashMapOf(
                        "name"             to nameField.text.toString().trim(),
                        "deadlineMillis"   to subDeadline,
                        "hasReminder"      to switchRem.isChecked,
                        "reminderOffsetMs" to offsetMs,
                        "assignToMe"       to assignMe,
                        "isDone"           to false
                    )
                    firestore.collection("tasks")
                        .document(taskId)
                        .collection("subtasks")
                        .add(subData)
                }

                if (isGroup && groupId != null) {
                    val groupDocRef = firestore.collection("groups").document(groupId)

                    groupDocRef.get().addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            // JOIN grup yang sudah ada
                            groupDocRef.update("members", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                                .addOnSuccessListener {
                                    firestore.collection("groups")
                                        .document(groupId)
                                        .collection("tasks")
                                        .document(taskId)
                                        .set(parentData)
                                }
                        } else {
                            // BUAT grup baru
                            val groupData = mapOf(
                                "createdBy" to uid,
                                "createdAt" to nowMs,
                                "members"   to listOf(uid)
                            )
                            groupDocRef.set(groupData)
                                .addOnSuccessListener {
                                    firestore.collection("groups")
                                        .document(groupId)
                                        .collection("tasks")
                                        .document(taskId)
                                        .set(parentData)
                                }
                        }
                    }
                }
                // Simpan ke kalender sistem
                startActivity(Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    putExtra(CalendarContract.Events.DESCRIPTION, desc)
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, selectedDeadlineMillis)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, selectedDeadlineMillis + 3_600_000)
                })

                // 8. Kembali ke MainActivity tanpa mengganggu fungsi lain
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { ex ->
                Toast.makeText(this, ex.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun addSubtaskView() {
        val inflater = LayoutInflater.from(this)
        val subtaskView = inflater.inflate(R.layout.layout_subtask, subtaskContainer, false)
        val etSubtaskDate = subtaskView.findViewById<TextInputEditText>(R.id.etSubtaskDate)
        val etSubtaskTime = subtaskView.findViewById<TextInputEditText>(R.id.etSubtaskTime)
        val switchReminder = subtaskView.findViewById<Switch>(R.id.switchReminder)
        val reminderOptions = subtaskView.findViewById<View>(R.id.reminderOptionsContainer)
        val assignContainer = subtaskView.findViewById<View>(R.id.assignContainer)
        val deleteBtn = subtaskView.findViewById<ImageView>(R.id.btnDeleteSubtask)

        // Assign to me hanya untuk Group
        assignContainer.visibility =
            if (rgTaskType.checkedRadioButtonId == R.id.rbGroup) View.VISIBLE else View.GONE

        // Subtask date/time pickers
        etSubtaskDate.setOnClickListener { showDatePickerDialog { y, m, d ->
            calendar.set(y, m, d)
            etSubtaskDate.setText(dateFormat.format(calendar.time))
        } }
        etSubtaskTime.setOnClickListener { showTimePickerDialog { h, mi ->
            calendar.set(Calendar.HOUR_OF_DAY, h)
            calendar.set(Calendar.MINUTE, mi)
            etSubtaskTime.setText(timeFormat.format(calendar.time))
        } }

        // Toggle reminder options
        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            reminderOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Delete subtask row
        deleteBtn.setOnClickListener {
            subtaskContainer.removeView(subtaskView)
        }

        subtaskContainer.addView(subtaskView)
    }

    private fun updateAssignSwitchVisibility() {
        for (i in 0 until subtaskContainer.childCount) {
            val subtaskView = subtaskContainer.getChildAt(i)
            val assignContainer = subtaskView.findViewById<View>(R.id.assignContainer)
            assignContainer.visibility =
                if (rgTaskType.checkedRadioButtonId == R.id.rbGroup) View.VISIBLE else View.GONE
        }
    }

    private fun showDatePickerDialog(onDateSet: (year: Int, month: Int, day: Int) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth -> onDateSet(year, month, dayOfMonth) },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePickerDialog(onTimeSet: (hour: Int, minute: Int) -> Unit) {
        val c = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute -> onTimeSet(hour, minute) },
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            true
        ).show()
    }

}
