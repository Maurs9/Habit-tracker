package org.isoron.uhabits.activities.common.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.provider.Settings
import android.text.method.DigitsKeyListener
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.dialogs.EntryDialogFragment.Companion.DRAFT_NOTES
import org.isoron.uhabits.activities.common.dialogs.EntryDialogFragment.Companion.DRAFT_VALUE
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.utils.formatEditableNumber
import org.isoron.uhabits.core.utils.parseFiniteNumber
import org.isoron.uhabits.databinding.CheckmarkPopupBinding
import org.isoron.uhabits.utils.InterfaceUtils
import org.isoron.uhabits.utils.requestFocusWithKeyboard
import org.isoron.uhabits.utils.sres
import kotlin.math.roundToInt

class NumberDialog : EntryDialogFragment() {

    var onToggle: ((Double, String) -> Unit)? = null
    var onDismiss: () -> Unit = {}

    private var originalNotes: String = ""
    private var originalValue: Double = 0.0
    private lateinit var view: CheckmarkPopupBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val prefs = appComponent.preferences
        view = CheckmarkPopupBinding.inflate(LayoutInflater.from(context))
        bindContext(view, numerical = true)
        arrayOf(view.yesBtn).forEach {
            it.setTextColor(requireArguments().getInt("color"))
        }
        arrayOf(view.noBtn, view.unknownBtnNumber).forEach {
            it.setTextColor(view.root.sres.getColor(R.attr.contrast60))
        }
        arrayOf(view.yesBtn, view.noBtn, view.unknownBtnNumber).forEach {
            it.typeface = InterfaceUtils.getFontAwesome(requireContext())
        }
        view.saveBtn.setEntryActionAccessibility(R.string.save)
        view.skipBtnNumber.setEntryActionAccessibility(R.string.entry_action_skip)
        view.unknownBtnNumber.setEntryActionAccessibility(R.string.entry_action_clear)
        if (!prefs.isSkipEnabled) view.skipBtnNumber.visibility = View.GONE
        if (!prefs.areQuestionMarksEnabled) view.unknownBtnNumber.visibility = View.GONE
        view.numberButtons.visibility = View.VISIBLE
        fixDecimalSeparator(view)
        originalNotes = requireArguments().getString("notes").orEmpty()
        originalValue = requireArguments().getDouble("value")
        view.notes.setText(savedInstanceState?.getString(DRAFT_NOTES) ?: originalNotes)
        view.value.setText(
            savedInstanceState?.getString(DRAFT_VALUE) ?: when {
                originalValue < 0 -> "0"
                else -> formatEditableNumber(originalValue, resources.configuration.locales[0])
            }
        )
        view.value.setOnKeyListener { _, keyCode, event ->
            if (event.action == MotionEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                save()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        view.saveBtn.setOnClickListener {
            save()
        }
        view.skipBtnNumber.setOnClickListener {
            submit(Entry.SKIP)
        }

        view.unknownBtnNumber.setOnClickListener {
            submit(Entry.UNKNOWN)
        }

        view.notes.setOnEditorActionListener { v, actionId, event ->
            save()
            true
        }
        view.value.requestFocusWithKeyboard()
        val dialog = Dialog(requireContext())
        dialog.setContentView(view.root)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::view.isInitialized) {
            outState.putString(DRAFT_NOTES, view.notes.text.toString())
            outState.putString(DRAFT_VALUE, view.value.text.toString())
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (activity?.isChangingConfigurations != true) onDismiss()
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        onToggle = null
        onDismiss = {}
        super.onDestroyView()
    }

    private fun fixDecimalSeparator(view: CheckmarkPopupBinding) {
        view.value.keyListener = DigitsKeyListener.getInstance(resources.configuration.locales[0], false, true)

        // https://github.com/flutter/flutter/issues/61175
        val currKeyboard = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        if (currKeyboard?.contains("swiftkey") == true || currKeyboard?.contains("samsung") == true) {
            view.value.inputType = EditorInfo.TYPE_CLASS_TEXT
        }
    }

    fun save() {
        val text = view.value.text.toString()
        if (text.isEmpty()) {
            submit(Entry.UNKNOWN)
            return
        }
        val locale = resources.configuration.locales[0]
        val value = parseFiniteNumber(text, locale)
        val maximum = Int.MAX_VALUE / 1000.0
        val error = when {
            text.isBlank() -> getString(R.string.validation_cannot_be_blank)
            value == null -> getString(R.string.habit_number_invalid)
            value < 0 -> getString(R.string.habit_number_nonnegative)
            value > maximum -> getString(R.string.habit_number_too_large, formatEditableNumber(maximum, locale))
            else -> null
        }
        view.value.error = error
        if (error != null) {
            view.value.requestFocus()
            return
        }
        submit((value!! * 1000).roundToInt())
    }

    private fun submit(value: Int) {
        val notes = view.notes.text.toString()
        val callback = onToggle?.let { { it(value / 1000.0, notes) } }
        if (submitEntry(value, notes, callback)) {
            dismiss()
        } else {
            view.value.error = getString(R.string.entry_habit_unavailable)
            view.value.requestFocus()
        }
    }
}
