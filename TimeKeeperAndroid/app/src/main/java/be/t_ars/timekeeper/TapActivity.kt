package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import be.t_ars.timekeeper.components.TapPartComponent
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.databinding.TapBinding
import java.io.Serializable

class TapActivity : AbstractActivity() {
    private lateinit var fBinding: TapBinding
    private lateinit var fTapPartComponent: TapPartComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = TapBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        fTapPartComponent = TapPartComponent(fBinding.tapPart, this::startSound, this::stopSound)
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getSettingScreenOrientation(this)
        loadIntent()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                OverviewActivity.startActivity(this)
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun loadIntent() {
        val extras = intent.extras
        if (extras != null) {
            val tempo = extras.getInt(kINTENT_DATA_TEMPO, ClickDescription.DEFAULT_TEMPO)
            val type = EClickType.of(extras.getInt(kINTENT_TYPE, EClickType.DEFAULT.value))
            val divisionCount = extras.getInt(kINTENT_DIVISION_COUNT, ClickDescription.DEFAULT_DIVISION_COUNT)
            val beatCount = extras.getInt(kINTENT_BEAT_COUNT, ClickDescription.DEFAULT_BEAT_COUNT)
            val countOff = extras.getBoolean(kINTENT_DATA_TEMPO, ClickDescription.DEFAULT_COUNT_OFF)

            fTapPartComponent.setTempo(tempo)
            fTapPartComponent.setClickType(type)
            fTapPartComponent.setDivisionCount(divisionCount)
            fTapPartComponent.setBeatCount(beatCount)
            fTapPartComponent.setCountOff(countOff)
        }
    }

    private fun startSound(click: ClickDescription) {
        val extras = HashMap<String, Serializable>().also {
            it[kINTENT_DATA_TEMPO] = click.bpm
            it[kINTENT_TYPE] = click.type.value
            it[kINTENT_DIVISION_COUNT] = click.divisionCount
            it[kINTENT_BEAT_COUNT] = click.beatCount
            it[kINTENT_COUNT_OFF] = click.countOff
        }
        SoundService.startSound(this, null, click, TapActivity::class.java, extras)
    }

    private fun stopSound() {
        SoundService.stopSound(this)
    }

    companion object {
        private const val kINTENT_DATA_TEMPO = "tempo"
        private const val kINTENT_TYPE = "type"
        private const val kINTENT_DIVISION_COUNT = "divisioncount"
        private const val kINTENT_BEAT_COUNT = "beatcount"
        private const val kINTENT_COUNT_OFF = "countoff"

        fun startActivity(context: Context) =
            Intent(context, TapActivity::class.java)
                .let(context::startActivity)
    }
}