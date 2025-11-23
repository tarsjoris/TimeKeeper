package be.t_ars.timekeeper.xr18

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun searchXR18(): InetAddress? {
    val socket = DatagramSocket().also { it.soTimeout = 5000 }
    try {
        socket.broadcast = true
        val payload = OSCMessage("/info").serialize()
        socket.send(
            DatagramPacket(
                payload,
                payload.size,
                InetAddress.getByName("255.255.255.255"),
                XR18OSCAPI.PORT
            )
        )

        val buffer = ByteArray(256)
        val udpPacket = DatagramPacket(buffer, buffer.size)
        socket.receive(udpPacket)
        val packet = parsePacket(udpPacket.data, udpPacket.length)
        if (packet is OSCMessage && packet.address == "/info") {
            return udpPacket.address
        }
    } catch (e: Throwable) {
        Log.e("TimeKeeper", "Got error while searching XR18", e)
    } finally {
        socket.close();
    }

    return null
}