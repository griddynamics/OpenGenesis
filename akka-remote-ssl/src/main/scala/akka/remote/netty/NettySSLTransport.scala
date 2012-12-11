package akka.remote.netty

import akka.remote._
import akka.actor.{Address, ActorRef, ActorSystemImpl}
import java.net.{InetAddress, InetSocketAddress}
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.handler.execution.ExecutionHandler
import org.jboss.netty.channel.group.{DefaultChannelGroup, ChannelGroup}
import org.jboss.netty.bootstrap.{ClientBootstrap, ServerBootstrap}
import akka.remote.RemoteProtocol.{CommandType, RemoteControlProtocol}
import org.jboss.netty.channel._
import org.jboss.netty.handler.ssl.SslHandler
import javax.net.ssl.{TrustManager, TrustManagerFactory, KeyManagerFactory, SSLContext}
import java.security.{GeneralSecurityException, SecureRandom, KeyStore}
import java.io.{FileNotFoundException, FileInputStream}
import org.jboss.netty.handler.codec.frame.{LengthFieldPrepender, LengthFieldBasedFrameDecoder}
import akka.event.{Logging, LoggingAdapter}
import collection.mutable.HashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import akka.remote.RemoteClientStarted
import scala.Some
import akka.remote.RemoteClientError
import akka.remote.RemoteServerError
import akka.remote.RemoteClientShutdown
import akka.remote.RemoteServerShutdown
import org.jboss.netty.handler.timeout.IdleStateHandler


class NettySSLTransport(override val remoteSettings: RemoteSettings, override val system: ActorSystemImpl, override val provider: RemoteActorRefProvider)
  extends NettyRemoteTransport(remoteSettings, system, provider){
  val sslSettings = new NettySSLAwareSettings(remoteSettings.config.getConfig("akka.remote.netty"), remoteSettings.systemName)
  override val server =  try
    if (sslSettings.EnableSSL) new NettyRemoteServerWithSSL(this)
    else new NettyRemoteServer(this)
  catch
    { case ex ⇒ shutdown(); throw ex }
  private val remoteClients = new HashMap[Address, RemoteClient]
  private val clientsLock = new ReentrantReadWriteLock

  override def send(
                     message: Any,
                     senderOption: Option[ActorRef],
                     recipient: RemoteActorRef): Unit = {

    val recipientAddress = recipient.path.address

    clientsLock.readLock.lock
    try {
      val client = remoteClients.get(recipientAddress) match {
        case Some(client) ⇒ client
        case None ⇒
          clientsLock.readLock.unlock
          clientsLock.writeLock.lock //Lock upgrade, not supported natively
          try {
            try {
              remoteClients.get(recipientAddress) match {
                //Recheck for addition, race between upgrades
                case Some(client) ⇒ client //If already populated by other writer
                case None ⇒ //Populate map
                  val client = new ActiveRemoteSSLClient(this, recipientAddress, address)
                  remoteClients += recipientAddress -> client
                  client
              }
            } finally {
              clientsLock.readLock.lock
            } //downgrade
          } finally {
            clientsLock.writeLock.unlock
          }
      }
      client.connect() // this will literally do nothing after the first time
      client.send(message, senderOption, recipient)
    } finally {
      clientsLock.readLock.unlock
    }
  }

  override def bindClient(remoteAddress: Address, client: RemoteClient, putIfAbsent: Boolean = false): Boolean = {
    clientsLock.writeLock().lock()
    try {
      if (putIfAbsent && remoteClients.contains(remoteAddress)) false
      else {
        client.connect()
        remoteClients.put(remoteAddress, client).foreach(_.shutdown())
        true
      }
    } finally {
      clientsLock.writeLock().unlock()
    }
  }

  override def unbindClient(remoteAddress: Address): Unit = {
    clientsLock.writeLock().lock()
    try {
      remoteClients foreach {
        case (k, v) ⇒
          if (v.isBoundTo(remoteAddress)) { v.shutdown(); remoteClients.remove(k) }
      }
    } finally {
      clientsLock.writeLock().unlock()
    }
  }

  override def shutdownClientConnection(remoteAddress: Address): Boolean = {
    clientsLock.writeLock().lock()
    try {
      remoteClients.remove(remoteAddress) match {
        case Some(client) ⇒ client.shutdown()
        case None         ⇒ false
      }
    } finally {
      clientsLock.writeLock().unlock()
    }
  }

  override def restartClientConnection(remoteAddress: Address): Boolean = {
    clientsLock.readLock().lock()
    try {
      remoteClients.get(remoteAddress) match {
        case Some(client) ⇒ client.connect(reconnectIfAlreadyConnected = true)
        case None         ⇒ false
      }
    } finally {
      clientsLock.readLock().unlock()
    }
  }

}

class NettyRemoteServerWithSSL(override val netty: NettySSLTransport) extends NettyRemoteServer(netty) {

  import netty.settings

  lazy val log = Logging(netty.system, "NettyRemoteServerWithSSL(" + ip + ")")

  private val factory = new NioServerSocketChannelFactory(
    Executors.newCachedThreadPool(netty.system.threadFactory),
    Executors.newCachedThreadPool(netty.system.threadFactory),
    settings.ServerSocketWorkerPoolSize)

  private val executionHandler = new ExecutionHandler(netty.executor)

  // group of open channels, used for clean-up
  private val openChannels: ChannelGroup = new DefaultDisposableChannelGroup("akka-remote-server")

  private val bootstrap = {
    val b = new ServerBootstrap(factory)
    b.setPipelineFactory(new SSLRemoteServerPipelineFactory(openChannels, executionHandler, netty, log))
    b.setOption("backlog", settings.Backlog)
    b.setOption("tcpNoDelay", true)
    b.setOption("child.keepAlive", true)
    b.setOption("reuseAddress", true)
    b
  }

  override def start(): Unit = {
    channel = bootstrap.bind(new InetSocketAddress(ip, settings.PortSelector))
    openChannels.add(channel)
  }

  override def shutdown() {
    try {
      val shutdownSignal = {
        val b = RemoteControlProtocol.newBuilder.setCommandType(CommandType.SHUTDOWN)
        b.setOrigin(RemoteProtocol.AddressProtocol.newBuilder
          .setSystem(netty.address.system)
          .setHostname(netty.address.host.get)
          .setPort(netty.address.port.get)
          .build)
        if (settings.SecureCookie.nonEmpty)
          b.setCookie(settings.SecureCookie.get)
        b.build
      }
      openChannels.write(netty.createControlEnvelope(shutdownSignal)).awaitUninterruptibly
      openChannels.disconnect
      openChannels.close.awaitUninterruptibly
      bootstrap.releaseExternalResources()
      netty.notifyListeners(RemoteServerShutdown(netty))
    } catch {
      case e: Exception ⇒ netty.notifyListeners(RemoteServerError(e, netty))
    }
  }
}


class SSLRemoteServerPipelineFactory(override val openChannels: ChannelGroup,
                                     override val executionHandler: ExecutionHandler,
                                     override val netty: NettySSLTransport,
                                     val log: LoggingAdapter) extends RemoteServerPipelineFactory(openChannels, executionHandler, netty) {
  import netty.settings
  import netty.sslSettings
  def initTLS(keyStorePath: String, keyStorePassword: String): Option[SSLContext] = {
    if (keyStorePath != null && keyStorePassword != null) {
      try {
        val factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
        val stream = new FileInputStream(keyStorePath)
        keyStore.load(stream, keyStorePassword.toCharArray)
        factory.init(keyStore, keyStorePassword.toCharArray)
        val sslContext = SSLContext.getInstance(sslSettings.SSLProtocol.get)
        sslContext.init(factory.getKeyManagers, null, new SecureRandom())
        Some(sslContext)
      } catch {
        case e: FileNotFoundException ⇒ {
          log.error(e, "TLS connection could not be established because keystore could not be loaded")
          None
        }
        case e: GeneralSecurityException ⇒ {
          log.error(e, "TLS connection could not be established")
          None
        }
      }
    } else {
      log.error("TLS connection could not be established because key store details are missing")
      None
    }
  }

  def getSSLHandler_? : Option[SslHandler] = {
    val sslContext: Option[SSLContext] = {
      if (sslSettings.EnableSSL) {
        log.debug("SSL is enabled, initialising...")
        initTLS(sslSettings.SSLKeyStore.get, sslSettings.SSLKeyStorePassword.get)
      } else {
        None
      }
    }
    if (sslContext.isDefined) {
      log.debug("Using SSL context to create SSLEngine...")
      val sslEngine = sslContext.get.createSSLEngine
      sslEngine.setUseClientMode(false)
      sslEngine.setEnabledCipherSuites(sslSettings.SSLSupportedAlgorithms.toArray.map(_.toString))
      Some(new SslHandler(sslEngine))
    } else {
      None
    }
  }


  override def getPipeline: ChannelPipeline = {
    val sslHandler = getSSLHandler_?
    val lenDec = new LengthFieldBasedFrameDecoder(settings.MessageFrameSize, 0, 4, 0, 4)
    val lenPrep = new LengthFieldPrepender(4)
    val messageDec = new RemoteMessageDecoder
    val messageEnc = new RemoteMessageEncoder(netty)

    val authenticator = if (settings.RequireCookie) new RemoteServerAuthenticationHandler(settings.SecureCookie) :: Nil else Nil
    val remoteServer = new RemoteServerHandler(openChannels, netty)
    val stages: List[ChannelHandler] = lenDec :: messageDec :: lenPrep :: messageEnc :: executionHandler :: authenticator ::: remoteServer :: Nil
    if (sslHandler.isDefined) {
      log.debug("Creating pipeline with SSL handler...")
      new StaticChannelPipeline(sslHandler.get :: stages: _*)
    } else {
      log.debug("Creating pipeline without SSL handler...")
      new StaticChannelPipeline(stages: _*)
    }
  }

}

class ActiveRemoteSSLClient private[akka](netty: NettySSLTransport,
                                          remoteAddress: Address,
                                          localAddress: Address) extends ActiveRemoteClient(netty, remoteAddress, localAddress) {

  import netty.settings
  import netty.sslSettings

  //TODO rewrite to a wrapper object (minimize volatile access and maximize encapsulation)
  @volatile
  private var bootstrap: ClientBootstrap = _
  @volatile
  private var connection: ChannelFuture = _
  @volatile
  private var executionHandler: ExecutionHandler = _

  @volatile
  private var reconnectionTimeWindowStart = 0L

  override def notifyListeners(msg: RemoteLifeCycleEvent): Unit = netty.notifyListeners(msg)

  override def currentChannel = connection.getChannel

  /**
   * Connect to remote server.
   */
  override def connect(reconnectIfAlreadyConnected: Boolean = false): Boolean = {

    def addSecureCookie(connection: ChannelFuture) {
      val handshake = RemoteControlProtocol.newBuilder.setCommandType(CommandType.CONNECT)
      if (settings.SecureCookie.nonEmpty) handshake.setCookie(settings.SecureCookie.get)
      handshake.setOrigin(RemoteProtocol.AddressProtocol.newBuilder
        .setSystem(localAddress.system)
        .setHostname(localAddress.host.get)
        .setPort(localAddress.port.get)
        .build)
      connection.getChannel.write(netty.createControlEnvelope(handshake.build))
    }

    def attemptReconnect(): Boolean = {
      val remoteIP = InetAddress.getByName(remoteAddress.host.get)
      log.debug("Remote client reconnecting to [{}|{}]", remoteAddress, remoteIP)
      connection = bootstrap.connect(new InetSocketAddress(remoteIP, remoteAddress.port.get))
      openChannels.add(connection.awaitUninterruptibly.getChannel) // Wait until the connection attempt succeeds or fails.

      if (!connection.isSuccess) {
        notifyListeners(RemoteClientError(connection.getCause, netty, remoteAddress))
        false
      } else {
        addSecureCookie(connection)
        true
      }
    }

    runSwitch switchOn {
      openChannels = new DefaultDisposableChannelGroup(classOf[RemoteClient].getName)

      executionHandler = new ExecutionHandler(netty.executor)
      val b = new ClientBootstrap(netty.clientChannelFactory)
      b.setPipelineFactory(new ActiveRemoteSSLClientPipelineFactory(name, b, executionHandler, remoteAddress, localAddress, this, sslSettings))
      b.setOption("tcpNoDelay", true)
      b.setOption("keepAlive", true)
      b.setOption("connectTimeoutMillis", settings.ConnectionTimeout.toMillis)
      settings.OutboundLocalAddress.foreach(s ⇒ b.setOption("localAddress", new InetSocketAddress(s, 0)))
      bootstrap = b

      val remoteIP = InetAddress.getByName(remoteAddress.host.get)
      log.debug("Starting remote client connection to [{}|{}]", remoteAddress, remoteIP)

      connection = bootstrap.connect(new InetSocketAddress(remoteIP, remoteAddress.port.get))

      openChannels.add(connection.awaitUninterruptibly.getChannel) // Wait until the connection attempt succeeds or fails.

      if (!connection.isSuccess) {
        notifyListeners(RemoteClientError(connection.getCause, netty, remoteAddress))
        false
      } else {
        addSecureCookie(connection)
        notifyListeners(RemoteClientStarted(netty, remoteAddress))
        true
      }
    } match {
      case true ⇒ true
      case false if reconnectIfAlreadyConnected ⇒
        connection.getChannel.close()
        openChannels.remove(connection.getChannel)

        log.debug("Remote client reconnecting to [{}]", remoteAddress)
        attemptReconnect()

      case false ⇒ false
    }
  }

  // Please note that this method does _not_ remove the ARC from the NettyRemoteClientModule's map of clients
  override def shutdown() = runSwitch switchOff {
    log.debug("Shutting down remote client [{}]", name)

    notifyListeners(RemoteClientShutdown(netty, remoteAddress))
    try {
      if ((connection ne null) && (connection.getChannel ne null))
        connection.getChannel.close()
    } finally {
      try {
        if (openChannels ne null) openChannels.close.awaitUninterruptibly()
      } finally {
        connection = null
        executionHandler = null
      }
    }

    log.debug("[{}] has been shut down", name)
  }

  private[akka] override def isWithinReconnectionTimeWindow: Boolean = {
    if (reconnectionTimeWindowStart == 0L) {
      reconnectionTimeWindowStart = System.currentTimeMillis
      true
    } else {
      val timeLeftMs = settings.ReconnectionTimeWindow.toMillis - (System.currentTimeMillis - reconnectionTimeWindowStart)
      val timeLeft = timeLeftMs > 0
      if (timeLeft)
        log.info("Will try to reconnect to remote server for another [{}] milliseconds", timeLeftMs)

      timeLeft
    }
  }

  private[akka] override def resetReconnectionTimeWindow = reconnectionTimeWindowStart = 0L
}


class ActiveRemoteSSLClientPipelineFactory(
                                            name: String,
                                            bootstrap: ClientBootstrap,
                                            executionHandler: ExecutionHandler,
                                            remoteAddress: Address,
                                            localAddress: Address,
                                            client: ActiveRemoteSSLClient, sslSettings: NettySSLAwareSettings) extends ChannelPipelineFactory {

  import client.netty.settings

  def initTLS(trustStorePath: String, trustStorePassword: String): Option[SSLContext] = {
    if (trustStorePath != null && trustStorePassword != null)
      try {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType)
        val stream = new FileInputStream(trustStorePath)
        trustStore.load(stream, trustStorePassword.toCharArray)
        trustManagerFactory.init(trustStore)
        val trustManagers: Array[TrustManager] = trustManagerFactory.getTrustManagers
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagers, new SecureRandom())
        Some(sslContext)
      } catch {
        case e: GeneralSecurityException ⇒ {
          client.log.error(e, "TLS connection could not be established. TLS is not used!");
          None
        }
      }
    else {
      client.log.error("TLS connection could not be established because trust store details are missing")
      None
    }
  }

  def getSSLHandler_? : Option[SslHandler] = {
    val sslContext: Option[SSLContext] = {
      if (sslSettings.EnableSSL) {
        client.log.debug("Client SSL is enabled, initialising ...")
        initTLS(sslSettings.SSLTrustStore.get, sslSettings.SSLTrustStorePassword.get)
      } else {
        None
      }
    }
    if (sslContext.isDefined) {
      client.log.debug("Client Using SSL context to create SSLEngine ...")
      val sslEngine = sslContext.get.createSSLEngine
      sslEngine.setUseClientMode(true)
      sslEngine.setEnabledCipherSuites(sslSettings.SSLSupportedAlgorithms.toArray.map(_.toString))
      Some(new SslHandler(sslEngine))
    } else {
      None
    }
  }

  def getPipeline: ChannelPipeline = {
    val sslHandler = getSSLHandler_?
    val timeout = new IdleStateHandler(client.netty.timer,
      settings.ReadTimeout.toSeconds.toInt,
      settings.WriteTimeout.toSeconds.toInt,
      settings.AllTimeout.toSeconds.toInt)
    val lenDec = new LengthFieldBasedFrameDecoder(settings.MessageFrameSize, 0, 4, 0, 4)
    val lenPrep = new LengthFieldPrepender(4)
    val messageDec = new RemoteMessageDecoder
    val messageEnc = new RemoteMessageEncoder(client.netty)
    val remoteClient = new ActiveRemoteClientHandler(name, bootstrap, remoteAddress, localAddress, client.netty.timer, client)
    val stages: List[ChannelHandler] = timeout :: lenDec :: messageDec :: lenPrep :: messageEnc :: executionHandler :: remoteClient :: Nil
    if (sslHandler.isDefined) {
      client.log.debug("Client creating pipeline with SSL handler...")
      new StaticChannelPipeline(sslHandler.get :: stages: _*)
    } else {
      client.log.debug("Client creating pipeline without SSL handler...")
      new StaticChannelPipeline(stages: _*)
    }
  }
}
