package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import be.t_ars.timekeeper.components.TapPartComponent
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
            val tempo = extras.getInt(kINTENT_DATA_TEMPO, 120)
            fBinding.tapPart.tempoSpinner.value = tempo
        }
    }

    private fun startSound(tempo: Int, clickType: EClickType) {
        val extras = HashMap<String, Serializable>().also {
            it[kINTENT_DATA_TEMPO] = tempo
        }
        SoundService.startSound(this, null, tempo, clickType, TapActivity::class.java, extras)
    }

    private fun stopSound() {
        SoundService.stopSound(this)
    }

    companion object {
        private const val kINTENT_DATA_TEMPO = "tempo"

        fun startActivity(context: Context, tempo: Int? = null) =
            Intent(context, TapActivity::class.java)
                .also { intent ->
                    tempo?.let { t -> intent.putExtra(kINTENT_DATA_TEMPO, t) }
                }
                .let(context::startActivity)
    }
}