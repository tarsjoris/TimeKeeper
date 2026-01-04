package be.t_ars.timekeeper

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import be.t_ars.timekeeper.components.TapPartComponent
import be.t_ars.timekeeper.data.ClickDetails
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

        fTapPartComponent = TapPartComponent(this, fBinding.tapPart) { click ->
            HashMap<String, Serializable>().also {
                it[kINTENT_DATA_CLICK] = click
            }
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

    @Suppress("DEPRECATION")
    private fun loadIntent() {
        val extras = intent.extras
        if (extras != null) {
            val click = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                extras.getSerializable(kINTENT_DATA_CLICK, ClickDetails::class.java)
            else
                extras.getSerializable(TapSongActivity.kINTENT_DATA_CLICK) as ClickDetails
            if (click != null) {
                fTapPartComponent.setClick(click)
            }
        }
    }

    companion object {
        private const val kINTENT_DATA_CLICK = "click"

        fun startActivity(context: Context) =
            Intent(context, TapActivity::class.java)
                .let(context::startActivity)
    }
}