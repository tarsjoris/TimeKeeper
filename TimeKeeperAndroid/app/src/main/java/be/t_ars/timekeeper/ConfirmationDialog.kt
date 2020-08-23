package be.t_ars.timekeeper

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ConfirmationDialog : DialogFragment() {
    private lateinit var fConfirmAction: (() -> Unit)
    private var fMessage: Int = 0
    private var fPositiveMessage: Int = 0
    private var fNegativeMessage: Int = 0

    fun setOptions(confirmAction: () -> Unit, message: Int, positiveMessage: Int, negativeMessage: Int) {
        fConfirmAction = confirmAction
        fMessage = message
        fPositiveMessage = positiveMessage
        fNegativeMessage = negativeMessage
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(fMessage)
                .setPositiveButton(fPositiveMessage) { _, _ -> fConfirmAction.invoke() }
                .setNegativeButton(fNegativeMessage, null)
        return builder.create()
    }
}