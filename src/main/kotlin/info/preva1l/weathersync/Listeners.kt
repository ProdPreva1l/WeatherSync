package info.preva1l.weathersync

import info.preva1l.weathersync.redis.UpdatePacket
import org.bukkit.WeatherType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.weather.WeatherChangeEvent

class Listeners : Listener {
    @EventHandler
    fun on(event: WeatherChangeEvent) {
        if (event.world.isClearWeather) {
            UpdatePacket(WeatherType.CLEAR, event.world.name)
            return
        }

        UpdatePacket(WeatherType.DOWNFALL, event.world.name)
        return
    }
}