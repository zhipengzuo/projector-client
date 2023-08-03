package org.jetbrains.projector.server.core.jwebsocket

import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.websocket.WsContext
import org.jetbrains.projector.common.protocol.toClient.MainWindow
import org.jetbrains.projector.server.core.ClientWrapper
import org.jetbrains.projector.server.core.util.getWildcardHostAddress
import org.jetbrains.projector.util.logging.Logger
import java.io.InputStream
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

public abstract class JHttpWsServer(host: InetAddress, port: Int) : JHttpWsTransport {

  public constructor(port: Int) : this(getWildcardHostAddress(), port)

  private val host: InetAddress = host
  private val port: Int = port

  private val connectMap = ConcurrentHashMap<WsContext, String>()

  public abstract fun getMainWindows(): List<MainWindow>

  private companion object {
    private val logger = Logger<JHttpWsServer>()
  }

  @Volatile
  private var wasInitialized = false

  private val wStarted: Boolean
    get() {
     return wasInitialized
    }

  private val wsServer = Javalin.create{
    it.staticFiles.add("/projector-client-web-distribution", Location.CLASSPATH)
  }.ws("/") {
    it.onConnect {
      ctx -> onOpen(ctx)
    }
    it.onMessage {
      ctx -> this@JHttpWsServer.onWsMessage(ctx, ctx.message())
    }
    it.onBinaryMessage {
      ctx -> this@JHttpWsServer.onWsMessage(ctx, ctx.data())
    }
    it.onClose {
      ctx -> this@JHttpWsServer.onWsClose(ctx)
    }
    it.onError {
      ctx -> this@JHttpWsServer.onError(ctx, ctx.error())
    }
  }.get("/mainWindows") {
    // endpoint of some static file that needs to handle path
    it.json(getMainWindows())
  }.get("/projector-client/{resourceFileName}") {

    val rfn = it.pathParam("resourceFileName")
    if (rfn.endsWith(".js")) {
      it.res().setHeader("Content-Type", "text/javascript")
    }

    it.result(getResource(it.pathParam("resourceFileName")))
  }.get("/projector/{resourceFileName}") {
    it.result(getResource(it.pathParam("resourceFileName")))
  }

  private fun onOpen(ctx: WsContext) {
    this.connectMap[ctx] = ""

    ctx.session.maxTextMessageSize = Long.MAX_VALUE
    ctx.session.maxBinaryMessageSize = Long.MAX_VALUE
    return this@JHttpWsServer.onWsOpen(ctx)
  }

  private fun getResource(resourceFileName: String): InputStream {
    try {
      return this::class.java.getResource("/projector-client-web-distribution/$resourceFileName")?.openStream()!!
    } catch (e: Exception) {
      logger.error(e) { "Wrong file $resourceFileName" }
    }
    return this::class.java.getResource("/projector-client-web-distribution/$resourceFileName")?.openStream()!!
  }

  override val wasStarted: Boolean by JHttpWsServer::wStarted

  override fun start() {
    wsServer.start(this@JHttpWsServer.host.hostAddress, this@JHttpWsServer.port)

    logger.info { "Server started on host $host and port $port" }
    this.wasInitialized = true
  }

  override fun stop(timeoutMs: Int) {
    wsServer.close()
  }

  override fun forEachOpenedConnection(action: (client: ClientWrapper) -> Unit) {
    connectMap.keys.filter { it.session.isOpen }.forEach{
      wsContext ->
      run {
        val sessionClientWrapper = wsContext.attribute<ClientWrapper>(CLIENT_WRAPPER)
        if (sessionClientWrapper != null) {
          try {
            action(sessionClientWrapper)
          } catch (e: Exception) {
            logger.error(e) { "ERROR: sessionClientWrapper is $sessionClientWrapper , wsContext is $wsContext" }
          }
        }
      }
    }
  }
}
