package be.t_ars.timekeeper.components

import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import android.widget.Button
import android.widget.EditText
import android.widget.SimpleAdapter
import android.widget.Spinner
import android.widget.TextView
import be.t_ars.timekeeper.AbstractActivity
import be.t_ars.timekeeper.InputDialog
import be.t_ars.timekeeper.R
import be.t_ars.timekeeper.data.ECue
import be.t_ars.timekeeper.data.Section
import be.t_ars.timekeeper.databinding.SectionsPartBinding
import kotlin.math.max
import kotlin.math.min

private const val kKEY_CUE = "cue"
private const val kKEY_BARCOUNT = "barcount"

private fun parseBarCount(text: CharSequence): Int {
    if (text.isBlank()) {
        return Section.DEFAULT_BARCOUNT
    }
    try {
        return min(1000, max(0, Integer.parseInt(text.toString())))
    } catch (e: NumberFormatException) {
        Log.e("TimeKeeper", "Invalid bar count: " + e.message, e)
    }
    return Section.DEFAULT_BARCOUNT
}

class SectionsPartComponent(
    private val context: AbstractActivity,
    private val sectionsPart: SectionsPartBinding
) {
    private val fSections: MutableList<Section> = ArrayList()
    private var fSelectedSection = -1
    private val fSectionsData: MutableList<Map<String, String>> = ArrayList()
    private val fAddSectionDialog: InputDialog = InputDialog()

    init {
        fAddSectionDialog.setOptions(
            { view ->
                val cueSpinner = view.findViewById<Spinner>(R.id.playlistedit_section_cue)
                val entries: MutableList<Map<String, String>> = ArrayList()
                entries.add(mapOf(kKEY_CUE to "<none>"))
                entries.addAll(ECue.values().map {
                    mapOf(kKEY_CUE to it.label)
                })
                cueSpinner.adapter = SimpleAdapter(
                    context,
                    entries,
                    R.layout.cue_entry,
                    arrayOf(kKEY_CUE),
                    intArrayOf(R.id.cue_name)
                )
                val barCountWidget = view.findViewById<EditText>(R.id.playlistedit_section_barcount)
                view.findViewById<Button>(R.id.playlistedit_section_barcount1_button)
                    .setOnClickListener {
                        barCountWidget.setText("1", TextView.BufferType.EDITABLE)
                    }
                view.findViewById<Button>(R.id.playlistedit_section_barcount2_button)
                    .setOnClickListener {
                        barCountWidget.setText("2", TextView.BufferType.EDITABLE)
                    }
                view.findViewById<Button>(R.id.playlistedit_section_barcount4_button)
                    .setOnClickListener {
                        barCountWidget.setText("4", TextView.BufferType.EDITABLE)
                    }
                view.findViewById<Button>(R.id.playlistedit_section_barcount8_button)
                    .setOnClickListener {
                        barCountWidget.setText("8", TextView.BufferType.EDITABLE)
                    }
                view.findViewById<Button>(R.id.playlistedit_section_barcount16_button)
                    .setOnClickListener {
                        barCountWidget.setText("16", TextView.BufferType.EDITABLE)
                    }

                if (fSelectedSection in fSections.indices) {
                    val section = fSections[fSelectedSection]
                    ECue.values().withIndex()
                        .find { indexedValue -> indexedValue.value == section.cue }
                        ?.index
                        ?.also { cueSpinner.setSelection(it + 1) }
                    barCountWidget.setText(
                        section.barCount.toString(),
                        TextView.BufferType.EDITABLE
                    )
                } else {
                    barCountWidget.setText(
                        Section.DEFAULT_BARCOUNT.toString(),
                        TextView.BufferType.EDITABLE
                    )
                }
            },
            { view ->
                val cueIndex =
                    view.findViewById<Spinner>(R.id.playlistedit_section_cue).selectedItemPosition - 1
                val cueValues = ECue.values()
                val cue = if (cueIndex in cueValues.indices) cueValues[cueIndex] else null
                val barCount = view.findViewById<EditText>(R.id.playlistedit_section_barcount).text
                val section = Section(parseBarCount(barCount), cue)
                if (fSelectedSection in fSections.indices) {
                    fSections.set(fSelectedSection, section)
                } else {
                    fSections.add(section)
                }
                reloadSections()
            },
            context.layoutInflater,
            R.string.playlistedit_action_addsection,
            R.drawable.ic_plus_dark,
            R.layout.section_dialog,
            R.string.add,
            R.string.cancel
        )

        sectionsPart.sectionsClose.setOnClickListener {
            sectionsPart.sectionsLayout.visibility = View.INVISIBLE
        }

        sectionsPart.sectionsList.adapter = SimpleAdapter(
            context,
            fSectionsData,
            R.layout.section_entry,
            arrayOf(kKEY_CUE, kKEY_BARCOUNT),
            intArrayOf(R.id.section_entry_cue, R.id.section_entry_barcount)
        )
        sectionsPart.sectionsList.setOnItemClickListener { _, _, position, _ ->
                if (position in fSections.indices) {
                    fSelectedSection = position
                    fAddSectionDialog.show(context.supportFragmentManager, "editsection")
                }
            }
        sectionsPart.sectionsList.setMultiChoiceModeListener(object :
            AbsListView.MultiChoiceModeListener {
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.sections_contextactions, menu)
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.sections_action_up -> {
                        move(true)
                    }

                    R.id.sections_action_down -> {
                        move(false)
                    }

                    R.id.sections_action_delete -> {
                        delete()
                        mode.finish()
                    }

                    else -> {
                        return false
                    }
                }
                return true
            }

            override fun onItemCheckedStateChanged(
                mode: ActionMode,
                position: Int,
                id: Long,
                checked: Boolean
            ) {
            }
        })

        sectionsPart.fabAddSection.setOnClickListener {
            fAddSectionDialog.show(context.supportFragmentManager, "addsection")
        }
    }

    fun setSections(sections: List<Section>) {
        fSections.clear()
        fSections.addAll(sections)
        reloadSections()
    }

    fun getSections() =
        fSections

    private fun move(up: Boolean) {
        val selection = sectionsPart.sectionsList.checkedItemPositions
        var ignore = false
        var i = if (up) 0 else fSections.size - 1
        while (if (up) i < fSections.size else i >= 0) {
            if (selection.get(i)) {
                if (if (up) i <= 0 else i >= fSections.size - 1) {
                    // leave the first selection group alone
                    ignore = true
                } else if (!ignore) {
                    val otherIndex = if (up) i - 1 else i + 1
                    if (!selection.get(otherIndex)) {
                        sectionsPart.sectionsList.setItemChecked(i, false)
                        sectionsPart.sectionsList.setItemChecked(otherIndex, true)
                    }
                    val otherPos = i + if (up) -1 else 1
                    if (i in 0 until fSections.size && otherPos in 0 until fSections.size) {
                        val song = fSections[i]
                        val other = fSections[otherPos]
                        fSections[i] = other
                        fSections[otherPos] = song
                    }
                }
            } else {
                ignore = false
            }
            i += if (up) 1 else -1
        }
        reloadSections()
    }

    private fun delete() {
        val selection = sectionsPart.sectionsList.checkedItemPositions
        fSections.indices.reversed().forEach { i ->
            if (selection.get(i)) {
                fSections.removeAt(i)
            }
        }
        reloadSections()
    }

    private fun reloadSections() {
        fSectionsData.clear()
        fSectionsData.addAll(
            fSections.map { section ->
                val cue = section.cue?.label ?: "-"
                val barCount = "${section.barCount}"
                mapOf(kKEY_CUE to cue, kKEY_BARCOUNT to barCount)
            }
        )
        (sectionsPart.sectionsList.adapter as SimpleAdapter).notifyDataSetChanged()
    }

    fun show() {
        sectionsPart.sectionsLayout.visibility = View.VISIBLE
    }
}