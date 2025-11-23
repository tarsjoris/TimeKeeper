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

fun showUltranetRouting(resources: Resources, ultranetPart: UltranetPartBinding) {
    GlobalScope.launch { updateChannels(resources, ultranetPart) }
}

private data class RoutingInfo(
    val routingSources: IntArray,
    val routingSourceNames: Array<String>,
    val routingSourceColors: IntArray
)

private fun updateChannels(resources: Resources, ultranetPart: UltranetPartBinding) {
    Log.i("TimeKeeper", "Searching for XR18")
    val address = searchXR18()
    if (address != null) {
        Log.i("TimeKeeper", "Found at $address")
        val (routingSources, routingSourceNames, routingSourceColors) = loadParameters(address)

        routingSources.forEachIndexed { index, routingSource ->
            val name = routingSourceNames[routingSource]
            val color = routingSourceColors[routingSource]
            val row = when (index.div(4)) {
                0 -> ultranetPart.ultranetRow1
                1 -> ultranetPart.ultranetRow2
                2 -> ultranetPart.ultranetRow3
                else -> ultranetPart.ultranetRow4
            }
            val cell = when (index.mod(4)) {
                0 -> row.ultranetEntry1
                1 -> row.ultranetEntry2
                2 -> row.ultranetEntry3
                else -> row.ultranetEntry4
            }
            val channelNumber = index + 1
            cell.channelNumber.text = "$channelNumber"
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
}

private fun loadParameters(address: InetAddress): RoutingInfo {
    val routingSources = IntArray(16)
    val routingSourceNames = Array<String>(XR18OSCAPI.ROUTING_SOURCE_COUNT) { "" }
    (XR18OSCAPI.ROUTING_SOURCE_USB1..XR18OSCAPI.ROUTING_SOURCE_USB18).forEach {
        routingSourceNames[it] = "USB ${it - XR18OSCAPI.ROUTING_SOURCE_USB1 + 1}"
    }
    val routingSourceColors = IntArray(XR18OSCAPI.ROUTING_SOURCE_COUNT)

    val xR18OSCAPI = XR18OSCAPI(address)
    try {
        val semaphore = Semaphore(0)
        val listener: IOSCListener = object : IOSCListener {
            override suspend fun p16RoutingSource(routing: Int, source: Int) {
                routingSources[routing - 1] = source
                semaphore.release()
            }

            override suspend fun channelName(channel: Int, name: String) {
                if (channel == 17) {
                    routingSourceNames[XR18OSCAPI.ROUTING_SOURCE_AUX_L] = "$name L"
                    routingSourceNames[XR18OSCAPI.ROUTING_SOURCE_AUX_R] = "$name R"
                } else {
                    routingSourceNames[channel - 1 + XR18OSCAPI.ROUTING_SOURCE_CHANNEL1] = name
                }
                semaphore.release()
            }

            override suspend fun channelColor(channel: Int, color: Int) {
                if (channel == 17) {
                    routingSourceColors[XR18OSCAPI.ROUTING_SOURCE_AUX_L] = color
                    routingSourceColors[XR18OSCAPI.ROUTING_SOURCE_AUX_R] = color
                } else {
                    routingSourceColors[channel - 1 + XR18OSCAPI.ROUTING_SOURCE_CHANNEL1] =
                        color
                }
                semaphore.release()
            }

            override suspend fun returnName(returnChannel: Int, name: String) {
                val leftIndex = (returnChannel - 1) * 2 + XR18OSCAPI.ROUTING_SOURCE_FX1_L
                routingSourceNames[leftIndex] = "$name L"
                routingSourceNames[leftIndex + 1] = "$name R"
                semaphore.release()
            }

            override suspend fun returnColor(returnChannel: Int, color: Int) {
                val leftIndex = (returnChannel - 1) * 2 + XR18OSCAPI.ROUTING_SOURCE_FX1_L
                routingSourceColors[leftIndex] = color
                routingSourceColors[leftIndex + 1] = color
                semaphore.release()
            }

            override suspend fun busName(bus: Int, name: String) {
                routingSourceNames[bus - 1 + XR18OSCAPI.ROUTING_SOURCE_BUS1] = name
                semaphore.release()
            }

            override suspend fun busColor(bus: Int, color: Int) {
                routingSourceColors[bus - 1 + XR18OSCAPI.ROUTING_SOURCE_BUS1] = color
                semaphore.release()
            }

            override suspend fun fxSendName(fxSend: Int, name: String) {
                routingSourceNames[fxSend - 1 + XR18OSCAPI.ROUTING_SOURCE_SEND1] = name
                semaphore.release()
            }

            override suspend fun fxSendColor(fxSend: Int, color: Int) {
                routingSourceColors[fxSend - 1 + XR18OSCAPI.ROUTING_SOURCE_SEND1] = color
                semaphore.release()
            }

            override suspend fun lrName(name: String) {
                routingSourceNames[XR18OSCAPI.ROUTING_SOURCE_L] = "$name L"
                routingSourceNames[XR18OSCAPI.ROUTING_SOURCE_R] = "$name R"
                semaphore.release()
            }

            override suspend fun lrColor(color: Int) {
                routingSourceColors[XR18OSCAPI.ROUTING_SOURCE_L] = color
                routingSourceColors[XR18OSCAPI.ROUTING_SOURCE_R] = color
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

    return RoutingInfo(routingSources, routingSourceNames, routingSourceColors)
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