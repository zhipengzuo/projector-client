package org.jetbrains.projector.server.core.jwebsocket

import io.javalin.websocket.WsContext
import org.jetbrains.projector.server.core.ServerTransport

public interface JHttpWsTransport : ServerTransport {
  override val clientCount: Int
    get() {
      var count = 0
      forEachOpenedConnection { count++ }
      return count
    }

  public fun onError(wsContext: WsContext?, e: Throwable?)
  public fun onWsOpen(wsContext: WsContext)
  public fun onWsClose(wsContext: WsContext)
  public fun onWsMessage(wsContext: WsContext, message: String)
  public fun onWsMessage(wsContext: WsContext, message: ByteArray)
}
