package org.jetbrains.projector.server.core.jwebsocket

import io.javalin.websocket.WsContext
import org.jetbrains.projector.server.core.CLIENT_SETTING
import org.jetbrains.projector.server.core.ClientSettings
import org.jetbrains.projector.server.core.ClientWrapper
import org.jetbrains.projector.server.core.ClosedClientSettings
import org.jetbrains.projector.util.logging.Logger
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

public const val CLIENT_WRAPPER: String = "CLIENT_WRAPPER"

public class JWsClientWrapper(private val wsContext: WsContext, initialSettings: ClientSettings): ClientWrapper {
  override var settings: ClientSettings = initialSettings

  override fun disconnect(reason: String) {
    val conn = wsContext
    conn.closeSession()

    val clientSettings = conn.attribute<ClientSettings>(CLIENT_SETTING)!!
    logger.info { "Disconnecting user ${clientSettings.address}. Reason: $reason" }
    conn.attribute(
      CLIENT_SETTING,
      ClosedClientSettings(
        connectionMillis = clientSettings.connectionMillis,
        address = clientSettings.address,
        reason = reason,
      )
    )
  }

  override fun send(data: ByteArray) {
    try {
      // Javalin ws send ByteArray will use send(Any), it may lead to fault, so using send(ByteBuffer)
      wsContext.send(ByteBuffer.wrap(data))
    }
    catch (e: Exception) {
      logger.debug(e) { "While generating message, client disconnected" }
    }
  }

  override val requiresConfirmation: Boolean
    get() {
      val remoteAddress = wsContext.session.remoteAddress
      if (remoteAddress is InetSocketAddress) {
        return remoteAddress.address?.isLoopbackAddress != true
      }
      return false
    }

  override val confirmationRemoteIp: InetAddress?
    get() {
      val remoteAddress = wsContext.session.remoteAddress
      if (remoteAddress is InetSocketAddress) {
        return remoteAddress.address
      }
      return null
    }

  override val confirmationRemoteName: String
    get() = "unknown host"

  private companion object {
    private val logger = Logger<JWsClientWrapper>()
  }
}
