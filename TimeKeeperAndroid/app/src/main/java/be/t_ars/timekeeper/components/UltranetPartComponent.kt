package be.t_ars.timekeeper.components

import android.content.res.Resources
import android.util.Log
import android.view.View
import be.t_ars.timekeeper.R
import be.t_ars.timekeeper.databinding.UltranetPartBinding
import be.t_ars.timekeeper.xr18.IOSCListener
import be.t_ars.timekeeper.xr18.XR18OSCAPI
import be.t_ars.timekeeper.xr18.searchXR18
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.div
import kotlin.text.get

fun showUltranetRouting(resources: Resources, ultranetPart: UltranetPartBinding) {
    GlobalScope.launch { updateChannels(resources, ultranetPart) }
}

private fun updateChannels(resources: Resources, ultranetPart: UltranetPartBinding) {
    Log.i("TimeKeeper", "Searching for XR18")
    val address = searchXR18()
    if (address != null) {
        Log.i("TimeKeeper", "Found at $address")
        loadParameters(address, resources, ultranetPart)
    }
}

private fun loadParameters(
    address: InetAddress,
    resources: Resources,
    ultranetPart: UltranetPartBinding
) {
    val routingSources = IntArray(16) { -1 }
    val routingSourceNames = Array(XR18OSCAPI.ROUTING_SOURCE_COUNT) {
        when (it) {
            in XR18OSCAPI.ROUTING_SOURCE_CHANNEL1..XR18OSCAPI.ROUTING_SOURCE_CHANNEL16
                -> "CH ${it - XR18OSCAPI.ROUTING_SOURCE_CHANNEL1 + 1}"

            XR18OSCAPI.ROUTING_SOURCE_AUX_L
                -> "AUX L"

            XR18OSCAPI.ROUTING_SOURCE_AUX_R
                -> "AUX R"

            in XR18OSCAPI.ROUTING_SOURCE_RTN1_L..XR18OSCAPI.ROUTING_SOURCE_RTN4_R
                -> "Rtn ${(it - XR18OSCAPI.ROUTING_SOURCE_RTN1_L).div(2) + 1} ${
                if ((it - XR18OSCAPI.ROUTING_SOURCE_RTN1_L).mod(
                        2
                    ) == 0
                ) "0" else "1"
            }"

            in XR18OSCAPI.ROUTING_SOURCE_BUS1..XR18OSCAPI.ROUTING_SOURCE_BUS6
                -> "Bus ${it - XR18OSCAPI.ROUTING_SOURCE_BUS1 + 1}"

            in XR18OSCAPI.ROUTING_SOURCE_SEND1..XR18OSCAPI.ROUTING_SOURCE_SEND4
                -> "FxSnd ${it - XR18OSCAPI.ROUTING_SOURCE_SEND1 + 1}"

            XR18OSCAPI.ROUTING_SOURCE_LR_L
                -> "LR L"

            XR18OSCAPI.ROUTING_SOURCE_LR_R
                -> "LR R"

            in XR18OSCAPI.ROUTING_SOURCE_DCA1..XR18OSCAPI.ROUTING_SOURCE_DCA4
                -> "DCA ${it - XR18OSCAPI.ROUTING_SOURCE_DCA1 + 1}"

            in XR18OSCAPI.ROUTING_SOURCE_USB1..XR18OSCAPI.ROUTING_SOURCE_USB14
                -> "USB ${it - XR18OSCAPI.ROUTING_SOURCE_USB1 + 1}"

            else
                -> ""
        }
    }
    val routingSourceColors = IntArray(XR18OSCAPI.ROUTING_SOURCE_COUNT)

    repeat(routingSources.size) { routingIndex ->
        val row = when (routingIndex.div(4)) {
            0 -> ultranetPart.ultranetRow1
            1 -> ultranetPart.ultranetRow2
            2 -> ultranetPart.ultranetRow3
            else -> ultranetPart.ultranetRow4
        }
        val cell = when (routingIndex.mod(4)) {
            0 -> row.ultranetEntry1
            1 -> row.ultranetEntry2
            2 -> row.ultranetEntry3
            else -> row.ultranetEntry4
        }
        val channelNumber = routingIndex + 1
        cell.channelNumber.text = "$channelNumber"
    }

    fun updateRouting(routingIndex: Int) {
        val routingSource = routingSources[routingIndex]
        if (routingSource != -1) {
            val name = routingSourceNames[routingSource]
            val color = routingSourceColors[routingSource]
            val row = when (routingIndex.div(4)) {
                0 -> ultranetPart.ultranetRow1
                1 -> ultranetPart.ultranetRow2
                2 -> ultranetPart.ultranetRow3
                else -> ultranetPart.ultranetRow4
            }
            val cell = when (routingIndex.mod(4)) {
                0 -> row.ultranetEntry1
                1 -> row.ultranetEntry2
                2 -> row.ultranetEntry3
                else -> row.ultranetEntry4
            }
            cell.channelName.text = name
            cell.channelName.setBackgroundResource(
                when (color) {
                    0 -> R.drawable.scribble_background_black
                    1 -> R.drawable.scribble_background_red
                    2 -> R.drawable.scribble_background_green
                    3 -> R.drawable.scribble_background_yellow
                    4 -> R.drawable.scribble_background_blue
                    5 -> R.drawable.scribble_background_magenta
                    6 -> R.drawable.scribble_background_cyan
                    7 -> R.drawable.scribble_background_white
                    8 -> R.drawable.scribble_background_black_inv
                    9 -> R.drawable.scribble_background_red_inv
                    10 -> R.drawable.scribble_background_green_inv
                    11 -> R.drawable.scribble_background_yellow_inv
                    12 -> R.drawable.scribble_background_blue_inv
                    13 -> R.drawable.scribble_background_magenta_inv
                    14 -> R.drawable.scribble_background_cyan_inv
                    else -> R.drawable.scribble_background_white_inv
                }
            )
            cell.channelName.setTextColor(
                resources.getColor(
                    when (color) {
                        0 -> R.color.scribble_white
                        1 -> R.color.scribble_black
                        2 -> R.color.scribble_black
                        3 -> R.color.scribble_black
                        4 -> R.color.scribble_black
                        5 -> R.color.scribble_black
                        6 -> R.color.scribble_black
                        7 -> R.color.scribble_black
                        8 -> R.color.scribble_white
                        9 -> R.color.scribble_red
                        10 -> R.color.scribble_green
                        11 -> R.color.scribble_yellow
                        12 -> R.color.scribble_blue
                        13 -> R.color.scribble_magenta
                        14 -> R.color.scribble_cyan
                        else -> R.color.scribble_white
                    }
                )
            )
        }
    }

    fun updateSource(sourceIndex: Int) {
        routingSources.indexOf(sourceIndex).let { if (it != -1) updateRouting(it) }
    }

    fun setName(sourceIndex: Int, name: String) {
        routingSourceNames[sourceIndex] = name
        updateSource(sourceIndex)
    }

    fun setColor(sourceIndex: Int, color: Int) {
        routingSourceColors[sourceIndex] = color
        updateSource(sourceIndex)
    }

    val xR18OSCAPI = XR18OSCAPI(address)
    try {
        val semaphore = Semaphore(0)
        val listener: IOSCListener = object : IOSCListener {
            override suspend fun p16RoutingSource(routing: Int, source: Int) {
                val routingIndex = routing - 1
                routingSources[routingIndex] = source
                semaphore.release()
                updateRouting(routingIndex)
            }

            override suspend fun channelName(channel: Int, name: String) {
                if (channel == 17) {
                    setName(XR18OSCAPI.ROUTING_SOURCE_AUX_L, "$name L")
                    setName(XR18OSCAPI.ROUTING_SOURCE_AUX_R, "$name R")
                } else {
                    setName(channel - 1 + XR18OSCAPI.ROUTING_SOURCE_CHANNEL1, name)
                }
                semaphore.release()
            }

            override suspend fun channelColor(channel: Int, color: Int) {
                if (channel == 17) {
                    setColor(XR18OSCAPI.ROUTING_SOURCE_AUX_L, color)
                    setColor(XR18OSCAPI.ROUTING_SOURCE_AUX_R, color)
                } else {
                    setColor(channel - 1 + XR18OSCAPI.ROUTING_SOURCE_CHANNEL1, color)
                }
                semaphore.release()
            }

            override suspend fun returnName(returnChannel: Int, name: String) {
                val leftIndex = (returnChannel - 1) * 2 + XR18OSCAPI.ROUTING_SOURCE_RTN1_L
                setName(leftIndex, "$name L")
                setName(leftIndex + 1, "$name R")
                semaphore.release()
            }

            override suspend fun returnColor(returnChannel: Int, color: Int) {
                val leftIndex = (returnChannel - 1) * 2 + XR18OSCAPI.ROUTING_SOURCE_RTN1_L
                setColor(leftIndex, color)
                setColor(leftIndex + 1, color)
                semaphore.release()
            }

            override suspend fun busName(bus: Int, name: String) {
                setName(bus - 1 + XR18OSCAPI.ROUTING_SOURCE_BUS1, name)
                semaphore.release()
            }

            override suspend fun busColor(bus: Int, color: Int) {
                setColor(bus - 1 + XR18OSCAPI.ROUTING_SOURCE_BUS1, color)
                semaphore.release()
            }

            override suspend fun fxSendName(fxSend: Int, name: String) {
                setName(fxSend - 1 + XR18OSCAPI.ROUTING_SOURCE_SEND1, name)
                semaphore.release()
            }

            override suspend fun fxSendColor(fxSend: Int, color: Int) {
                setColor(fxSend - 1 + XR18OSCAPI.ROUTING_SOURCE_SEND1, color)
                semaphore.release()
            }

            override suspend fun lrName(name: String) {
                setName(XR18OSCAPI.ROUTING_SOURCE_LR_L, "$name L")
                setName(XR18OSCAPI.ROUTING_SOURCE_LR_R, "$name R")
                semaphore.release()
            }

            override suspend fun lrColor(color: Int) {
                setColor(XR18OSCAPI.ROUTING_SOURCE_LR_L, color)
                setColor(XR18OSCAPI.ROUTING_SOURCE_LR_R, color)
                semaphore.release()
            }
        }
        xR18OSCAPI.addListener(listener)
        GlobalScope.launch { xR18OSCAPI.handleResponses() }
        repeat(16) {
            requestParameter(semaphore) { xR18OSCAPI.requestP16RoutingSource(it + 1) }
        }
        repeat(XR18OSCAPI.CHANNEL_COUNT) {
            requestParameter(semaphore) { xR18OSCAPI.requestChannelName(it + 1) }
            requestParameter(semaphore) { xR18OSCAPI.requestChannelColor(it + 1) }
        }
        repeat(XR18OSCAPI.RETURN_COUNT) {
            requestParameter(semaphore) { xR18OSCAPI.requestReturnName(it + 1) }
            requestParameter(semaphore) { xR18OSCAPI.requestReturnColor(it + 1) }
        }
        repeat(XR18OSCAPI.BUS_COUNT) {
            requestParameter(semaphore) { xR18OSCAPI.requestBusName(it + 1) }
            requestParameter(semaphore) { xR18OSCAPI.requestBusColor(it + 1) }
        }
        repeat(XR18OSCAPI.FXSEND_COUNT) {
            requestParameter(semaphore) { xR18OSCAPI.requestFXSendName(it + 1) }
            requestParameter(semaphore) { xR18OSCAPI.requestFXSendColor(it + 1) }
        }
        requestParameter(semaphore) { xR18OSCAPI.requestLRName() }
        requestParameter(semaphore) { xR18OSCAPI.requestLRColor() }
    } catch (e: Exception) {
        Log.e("TimeKeeper", "Got error while loading parameters", e)
    } finally {
        xR18OSCAPI.stop()
    }
}

private fun requestParameter(semaphore: Semaphore, request: () -> Unit) {
    var tries = 0
    do {
        if (++tries > 10) {
            Log.e("TimeKeepeer", "Could not request parameter")
            continue
        }
        request()
    } while (!semaphore.tryAcquire(1, TimeUnit.SECONDS))
}