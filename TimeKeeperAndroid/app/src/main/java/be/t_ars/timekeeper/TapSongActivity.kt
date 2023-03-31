package be.t_ars.timekeeper

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import be.t_ars.timekeeper.components.TapPartComponent
import be.t_ars.timekeeper.data.ClickDescription
import be.t_ars.timekeeper.data.EClickType
import be.t_ars.timekeeper.databinding.TapSongBinding

class TapSongActivity : AbstractActivity() {
    private lateinit var fBinding: TapSongBinding
    private lateinit var fTapPartComponent: TapPartComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = TapSongBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        fTapPartComponent = TapPartComponent(fBinding.tapPart, this::startSound, this::stopSound)
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = getSettingScreenOrientation(this)
        loadIntent()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tap_song_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tap_action_accept -> {
                fBinding.tapPart.tempoSpinner.clearFocus()

                val intent = Intent().also {
                    it.putExtra(kINTENT_DATA_NAME, fBinding.name.text.toString())
                    it.putExtra(kINTENT_DATA_SCORE_LINK, fBinding.scoreLink.text.toString())
                    it.putExtra(kINTENT_DATA_TEMPO, fTapPartComponent.getTempo())
                    it.putExtra(kINTENT_DATA_CLICK_TYPE, fTapPartComponent.getClickType().value)
                    it.putExtra(kINTENT_DATA_COUNT_OFF, fTapPartComponent.isCountOff())
                }
                setResult(RESULT_OK, intent)
                finish()
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun loadIntent() {
        fBinding.name.setText(intent.getStringExtra(kINTENT_DATA_NAME))

        val newTempo = intent.getIntExtra(kINTENT_DATA_TEMPO, ClickDescription.DEFAULT_TEMPO)
        fTapPartComponent.setTempo(newTempo)

        val newClickType =
            intent.getIntExtra(kINTENT_DATA_CLICK_TYPE, EClickType.DEFAULT.value)
                .let(EClickType::of)
        fTapPartComponent.setClickType(newClickType)

        val newCountOff = intent.getBooleanExtra(kINTENT_DATA_COUNT_OFF, false)
        fTapPartComponent.setCountOff(newCountOff)

        fBinding.scoreLink.setText(intent.getStringExtra(kINTENT_DATA_SCORE_LINK) ?: "")
    }

    private fun startSound(click: ClickDescription) {
        SoundService.startSound(this, null, click)
    }

    private fun stopSound() {
        SoundService.stopSound(this)
    }


    companion object {
        const val kINTENT_DATA_TEMPO = "tempo"
        const val kINTENT_DATA_CLICK_TYPE = "click_type"
        const val kINTENT_DATA_COUNT_OFF = "count_off"
        const val kINTENT_DATA_NAME = "name"
        const val kINTENT_DATA_SCORE_LINK = "score_link"

        fun startActivityForResult(
            context: FragmentActivity,
            click: ClickDescription,
            name: String,
            scoreLink: String?,
            requestCode: Int
        ) =
            Intent(context, TapSongActivity::class.java)
                .also { intent ->
                    intent.putExtra(kINTENT_DATA_TEMPO, click.bpm)
                    intent.putExtra(kINTENT_DATA_CLICK_TYPE, click.type.value)
                    intent.putExtra(kINTENT_DATA_COUNT_OFF, click.countOff)
                    intent.putExtra(kINTENT_DATA_NAME, name)
                    if (scoreLink != null)
                        intent.putExtra(kINTENT_DATA_SCORE_LINK, scoreLink)
                }
                .let { context.startActivityForResult(it, requestCode) }
    }
}