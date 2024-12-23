package info.preva1l.weathersync.redis

import info.preva1l.weathersync.Config
import info.preva1l.weathersync.WeatherSync
import org.jetbrains.annotations.Blocking
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisPubSub
import redis.clients.jedis.exceptions.JedisException
import redis.clients.jedis.util.Pool
import java.util.logging.Level

class RedisBroker : Broker() {
    private val subscriber: Subscriber
    private var channel: String = "NONE"

    init {
        subscriber = Subscriber()
    }

    @Blocking
    override fun connect() {
        val jedisPool: Pool<Jedis> = getJedisPool()
        try {
            jedisPool.resource.ping()
        } catch (e: JedisException) {
            throw IllegalStateException(
                "Failed to establish connection with Redis. "
                        + "Please check the supplied credentials in the config file", e
            )
        }

        subscriber.enable(jedisPool)
        val thread = Thread({ subscriber.subscribe() }, "weathersync:redis_subscriber")
        thread.isDaemon = true
        thread.start()
    }

    override fun update(packet: UpdatePacket) {
        subscriber.send(packet)
    }

    override fun destroy() {
        subscriber.disable()
    }

    private fun getJedisPool(): Pool<Jedis> {
        val conf: Config.Broker? = Config.i()?.broker
        val password: String = conf?.password ?: ""
        val host: String = conf?.host ?: ""
        val port: Int = conf?.port ?: 0
        channel = conf?.channel ?: channel

        val config = JedisPoolConfig()
        config.maxIdle = 20
        config.maxTotal = 50
        config.testOnBorrow = true
        config.testOnReturn = true

        return if (password.isEmpty()
        ) JedisPool(config, host, port, 0, false)
        else JedisPool(config, host, port, 0, password, false)
    }

    private inner class Subscriber : JedisPubSub() {
        private val reconnectionTime = 8000

        private var jedisPool: Pool<Jedis>? = null
        private var enabled = false
        private var reconnected = false

        fun enable(jedisPool: Pool<Jedis>) {
            this.jedisPool = jedisPool
            this.enabled = true
        }

        @Blocking
        fun disable() {
            this.enabled = false
            if (jedisPool != null && !jedisPool!!.isClosed) {
                jedisPool!!.close()
            }
            this.unsubscribe()
        }

        @Blocking
        fun send(packet: UpdatePacket) {
            try {
                jedisPool!!.resource.use { jedis ->
                    jedis.publish(channel, gson.toJson(packet))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @Blocking
        private fun subscribe() {
            while (enabled && !Thread.interrupted() && jedisPool != null && !jedisPool!!.isClosed) {
                try {
                    jedisPool!!.resource.use { jedis ->
                        if (reconnected) {
                            WeatherSync.i()?.logger?.info("Redis connection is alive again")
                        }
                        jedis.subscribe(this, channel)
                    }
                } catch (t: Throwable) {
                    onThreadUnlock(t)
                }
            }
        }

        private fun onThreadUnlock(t: Throwable) {
            if (!enabled) {
                return
            }

            if (reconnected) {
                WeatherSync.i()?.logger?.log(
                    Level.WARNING, "Redis Server connection lost. Attempting reconnect in ${reconnectionTime/1000}...", t
                )
            }
            try {
                this.unsubscribe()
            } catch (ignored: Throwable) {
            }

            if (!reconnected) {
                reconnected = true
            } else {
                try {
                    Thread.sleep(reconnectionTime.toLong())
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }

        override fun onMessage(channel: String, encoded: String) {
            if (channel != this@RedisBroker.channel) {
                return
            }
            val packet: UpdatePacket

            try {
                packet = this@RedisBroker.gson.fromJson(encoded, UpdatePacket::class.java)
            } catch (e: Exception) {
                WeatherSync.i()?.logger?.warning("Failed to decode message from Redis: " + e.message)
                return
            }

            try {
                this@RedisBroker.handle(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}