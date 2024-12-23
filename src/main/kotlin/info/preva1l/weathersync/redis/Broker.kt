package info.preva1l.weathersync.redis

import com.google.gson.Gson
import info.preva1l.weathersync.Config
import org.bukkit.Bukkit
import org.bukkit.WeatherType

abstract class Broker {
    protected val gson: Gson = Gson()

    protected fun handle(packet: UpdatePacket) {
        when (packet.weather) {
            WeatherType.CLEAR -> {
                Bukkit.getWorld(packet.world)?.clearWeatherDuration = Integer.MAX_VALUE
                Bukkit.getWorld(packet.world)?.setStorm(false)
            }
            WeatherType.DOWNFALL -> {
                Bukkit.getWorld(packet.world)?.clearWeatherDuration = 0
                Bukkit.getWorld(packet.world)?.setStorm(true)
            }
        }
    }

    abstract fun connect()
    abstract fun update(packet: UpdatePacket)
    abstract fun destroy()

    enum class Type(val displayName: String) {
        REDIS("Redis")
    }

    companion object {
        private var instance: Broker? = null

        fun obtain(): Broker? {
            if (instance == null) {
                when (Config.i()?.broker?.type!!) {
                    Type.REDIS -> instance = RedisBroker()
                }
            }
            return instance
        }
    }
}