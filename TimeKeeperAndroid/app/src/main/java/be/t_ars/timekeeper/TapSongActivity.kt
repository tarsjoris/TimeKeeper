package be.t_ars.timekeeper
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.FragmentActivity
import be.t_ars.timekeeper.components.TapPartComponent
import be.t_ars.timekeeper.databinding.TapSongBinding

class TapSongActivity : AbstractActivity() {
    private lateinit var fBinding: TapSongBinding
    private lateinit var fTapPartComponent: TapPartComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding = TapSongBinding.inflate(layoutInflater)
        setContentView(fBinding.root)
        setSupportActionBar(fBinding.toolbar)

        fBinding.tapPart.checkboxWithTempo.visibility = View.VISIBLE

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

                Intent().also {
                    it.putExtra(kINTENT_DATA_NAME, fBinding.name.text.toString())
                    it.putExtra(kINTENT_DATA_SCORE_LINK, fBinding.scoreLink.text.toString())
                    it.putExtra(kINTENT_DATA_TEMPO, fTapPartComponent.getTempo())
                    it.putExtra(kINTENT_DATA_DIVISIONS, fTapPartComponent.getDivisions())
                    setResult(RESULT_OK, it)
                }
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

        val newTempo = intent.getIntExtra(kINTENT_DATA_TEMPO, -1)
        val withTempo = newTempo != -1
        fBinding.tapPart.checkboxWithTempo.isChecked = withTempo
        fTapPartComponent.setTempo(if (withTempo) newTempo else 120)

        val newDivisions = intent.getIntExtra(kINTENT_DATA_DIVISIONS, 1)
        fTapPartComponent.setDivisions(newDivisions)

        fBinding.scoreLink.setText(intent.getStringExtra(kINTENT_DATA_SCORE_LINK) ?: "")
    }

    private fun startSound(tempo: Int, divisions: Int) {
        SoundService.startSound(this, null, tempo, divisions)
    }

    private fun stopSound() {
        SoundService.stopSound(this)
    }


    companion object {
        const val kINTENT_DATA_TEMPO = "tempo"
        const val kINTENT_DATA_DIVISIONS = "divisions"
        const val kINTENT_DATA_NAME = "name"
        const val kINTENT_DATA_SCORE_LINK = "score_link"

        fun startActivityForResult(
            context: FragmentActivity,
            tempo: Int?,
            divisions: Int,
            name: String,
            scoreLink: String?,
            requestCode: Int
        ) =
            Intent(context, TapSongActivity::class.java)
                .also { intent ->
                    intent.putExtra(kINTENT_DATA_TEMPO, tempo)
                    intent.putExtra(kINTENT_DATA_DIVISIONS, divisions)
                    intent.putExtra(kINTENT_DATA_NAME, name)
                    if (scoreLink != null)
                        intent.putExtra(kINTENT_DATA_SCORE_LINK, scoreLink)
                }
                .let { context.startActivityForResult(it, requestCode) }
    }
}