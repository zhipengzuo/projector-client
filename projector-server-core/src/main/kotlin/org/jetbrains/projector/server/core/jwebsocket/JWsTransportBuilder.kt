package org.jetbrains.projector.server.core.jwebsocket

import io.javalin.websocket.WsContext
import org.jetbrains.projector.server.core.ClientEventHandler
import org.jetbrains.projector.server.core.ClientWrapper
import org.jetbrains.projector.util.logging.Logger
import java.net.InetSocketAddress

public abstract class JWsTransportBuilder {
  public lateinit var onError: (WsContext?, Throwable?) -> Unit
  public lateinit var onWsOpen: (wsContext: WsContext) -> Unit
  public lateinit var onWsClose: (wsContext: WsContext) -> Unit
  public lateinit var onWsMessageString: (wsContext: WsContext, message: String) -> Unit
  public lateinit var onWsMessageByteBuffer: (wsContext: WsContext, message: ByteArray) -> Unit

  public abstract fun build(): JHttpWsTransport

  public fun attachDefaultServerEventHandlers(clientEventHandler: ClientEventHandler): JWsTransportBuilder {
    onWsMessageByteBuffer = { _, message ->
      throw RuntimeException("Unsupported message type: $message")
    }

    onWsMessageString = { wsContext, message ->
      clientEventHandler.handleMessage(wsContext.attribute<ClientWrapper>(CLIENT_WRAPPER)!!, message)
    }

    onWsClose = { wsContext ->
      // todo: we need more informative message, add parameters to this method inside the superclass
      clientEventHandler.updateClientsCount()
      val wrapper = wsContext.attribute<ClientWrapper>(CLIENT_WRAPPER)
      if (wrapper != null) {
        clientEventHandler.onClientConnectionEnded(wrapper)
      } else {
        logger.info {
          val address = wsHostAddress(wsContext)
          "Client from address $address is disconnected. This client hasn't clientSettings. " +
          "This usually happens when the handshake stage didn't have time to be performed " +
          "(so it seems the client has been connected for a very short time)"
        }
      }
    }

    onWsOpen = { wsContext ->
      val address = wsHostAddress(wsContext)
      val wrapper = JWsClientWrapper(wsContext, clientEventHandler.getInitialClientState(address))
      wsContext.attribute(CLIENT_WRAPPER, wrapper)
      logger.info { "$address connected." }
      clientEventHandler.onClientConnected(wrapper)
    }

    onError = { _, e ->
      logger.error(e) { "onError" }
    }

    return this
  }

  private fun wsHostAddress(wsContext: WsContext): String? {
    val remoteAddress = wsContext.session.remoteAddress
    var address: String? = "UNKNOWN"
    if (remoteAddress is InetSocketAddress) {
      address = remoteAddress.address?.hostAddress
    }
    return address
  }

  private companion object {
    private val logger = Logger<JWsTransportBuilder>()
  }
}
