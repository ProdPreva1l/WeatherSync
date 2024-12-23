package info.preva1l.weathersync

import de.exlll.configlib.*
import java.io.File
import java.nio.charset.StandardCharsets

@Configuration
class Config {
    val broker = Broker()

    @Configuration
    class Broker {
        val enabled = false

        @Comment("Allowed: REDIS")
        val type: info.preva1l.weathersync.redis.Broker.Type = info.preva1l.weathersync.redis.Broker.Type.REDIS
        val host = "localhost"
        val port = 6379
        val password = "myAwesomePassword"
        val channel = "weathersync:update"
    }

    companion object {
        private val properties = YamlConfigurationProperties.newBuilder()
            .charset(StandardCharsets.UTF_8)
            .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
            .build()

        private var instance: Config? = null

        fun i(): Config? {
            if (instance == null)
                instance = YamlConfigurations.update(
                    File(WeatherSync.i()?.dataFolder, "config.yml").toPath(),
                    Config::class.java, properties
                )
            return instance
        }
    }
}