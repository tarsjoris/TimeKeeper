package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import be.t_ars.timekeeper.components.TapPartComponent
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.databinding.TapBinding
import java.io.Serializable

class TapActivity : AbstractActivity() {
    private lateinit var fBinding: TapBinding
    private lateinit var fTapPartComponent: TapPartComponent
    private var playing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = TapBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        fTapPartComponent = TapPartComponent(fBinding.tapPart, this::clickChanged)

        fBinding.buttonStart.setOnClickListener {
            playing = true
            startSound()
        }
        fBinding.buttonStop.setOnClickListener {
            playing = false
            stopSound()
        }
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
            val click = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                extras.getSerializable(kINTENT_DATA_CLICK, ClickDescription::class.java)
            else
                extras.getSerializable(TapSongActivity.kINTENT_DATA_CLICK) as ClickDescription
            if (click != null) {
                fTapPartComponent.setClick(click)
            }
        }
    }

    private fun clickChanged() {
        if (playing) {
            startSound()
        }
    }

    private fun startSound() {
        val click = fTapPartComponent.getClick()
        val extras = HashMap<String, Serializable>().also {
            it[kINTENT_DATA_CLICK] = click
        }
        SoundService.startSound(this, null, click, TapActivity::class.java, extras)
    }

    private fun stopSound() {
        SoundService.stopSound(this)
    }

    companion object {
        private const val kINTENT_DATA_CLICK = "click"

        fun startActivity(context: Context) =
            Intent(context, TapActivity::class.java)
                .let(context::startActivity)
    }
}