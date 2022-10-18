package org.jetbrains.projector.server.core.jwebsocket

import io.javalin.websocket.WsContext
import org.jetbrains.projector.common.protocol.toClient.MainWindow
import java.net.InetAddress

public class JHttpWsServerBuilder(private val host: InetAddress, private val port: Int): JWsTransportBuilder() {

  public lateinit var getMainWindows: () -> List<MainWindow>

  override fun build(): JHttpWsServer {

    return object : JHttpWsServer(host, port) {
      override fun getMainWindows(): List<MainWindow> = this@JHttpWsServerBuilder.getMainWindows()
      override fun onError(wsContext: WsContext?, e: Throwable?) = this@JHttpWsServerBuilder.onError(wsContext, e)
      override fun onWsOpen(wsContext: WsContext) = this@JHttpWsServerBuilder.onWsOpen(wsContext)
      override fun onWsClose(wsContext: WsContext) = this@JHttpWsServerBuilder.onWsClose(wsContext)
      override fun onWsMessage(wsContext: WsContext, message: String) = this@JHttpWsServerBuilder.onWsMessageString(wsContext, message)
      override fun onWsMessage(wsContext: WsContext, message: ByteArray) = this@JHttpWsServerBuilder.onWsMessageByteBuffer(wsContext,
                                                                                                                           message)
    }
  }
}
