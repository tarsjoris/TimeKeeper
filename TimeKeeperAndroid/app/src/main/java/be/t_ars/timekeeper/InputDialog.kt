package be.t_ars.timekeeper

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View

class InputDialog : DialogFragment() {
    private var fViewCreatedAction: ((View) -> Unit)? = null
    private lateinit var fConfirmAction: ((View) -> Unit)
    private lateinit var fLayoutInflater: LayoutInflater
    private var fTitleID: Int = 0
    private var fIconID: Int = 0
    private var fViewID: Int = 0
    private var fPositiveTextID: Int = 0
    private var fNegativeTextID: Int = 0

    fun setOptions(viewCreatedAction: ((View) -> Unit)?, confirmAction: (View) -> Unit, layoutInflater: LayoutInflater, titleID: Int, iconID: Int, viewID: Int, positiveTextID: Int, negativeTextID: Int) {
        fViewCreatedAction = viewCreatedAction
        fConfirmAction = confirmAction
        fLayoutInflater = layoutInflater
        fTitleID = titleID
        fIconID = iconID
        fViewID = viewID
        fPositiveTextID = positiveTextID
        fNegativeTextID = negativeTextID
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
                .setTitle(fTitleID)
                .setIcon(fIconID)
        val view = fLayoutInflater.inflate(fViewID, null)
        fViewCreatedAction?.invoke(view)
        builder.setView(view)
                .setPositiveButton(fPositiveTextID) { _, _ -> fConfirmAction.invoke(view) }
                .setNegativeButton(fNegativeTextID, null)
        return builder.create()
    }
}
