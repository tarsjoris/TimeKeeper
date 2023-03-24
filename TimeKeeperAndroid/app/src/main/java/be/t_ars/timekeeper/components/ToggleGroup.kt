package be.t_ars.timekeeper.components

import android.widget.ToggleButton

class ToggleEntry<T>(val value: T, val view: ToggleButton)

class ToggleGroup<T>(private val toggles: Array<ToggleEntry<T>>, selectionChanged: (T) -> Unit) {
    private var currentValue = toggles[0].value

    init {
        toggles.forEach {  toggleEntry ->
            toggleEntry.view.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    setValue(toggleEntry.value)
                    selectionChanged(toggleEntry.value)
                } else if (toggleEntry.value == currentValue) {
                    toggleEntry.view.isChecked = true
                }
            }
        }
    }

    fun setValue(value: T): Boolean {
        currentValue = value
        toggles.forEach {
            it.view.isChecked = it.value == value
        }
        return toggles.any { it.value == value }
    }
}