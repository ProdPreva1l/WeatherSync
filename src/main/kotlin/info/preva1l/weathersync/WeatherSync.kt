package info.preva1l.weathersync

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class WeatherSync : JavaPlugin() {
    override fun onEnable() {
        instance = this
        Bukkit.getPluginManager().registerEvents(Listeners(), this)
    }

    companion object {
        var instance: WeatherSync? = null

        fun i() = instance
    }
}
