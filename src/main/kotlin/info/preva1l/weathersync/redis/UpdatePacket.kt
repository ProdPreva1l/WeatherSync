package info.preva1l.weathersync.redis

import com.google.gson.annotations.Expose
import org.bukkit.WeatherType

data class UpdatePacket(
    @Expose val weather: WeatherType,
    @Expose val world: String
) {
    fun send() {
        Broker.obtain()?.update(this)
    }
}