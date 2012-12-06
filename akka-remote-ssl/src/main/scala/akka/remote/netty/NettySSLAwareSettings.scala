package akka.remote.netty

import com.typesafe.config.Config
import akka.config.ConfigurationException


class NettySSLAwareSettings(config: Config, override val systemName: String) extends NettySettings(config, systemName) {
  import config._
  val SSLKeyStore = getString("ssl-key-store") match {
    case ""       ⇒ None
    case keyStore ⇒ Some(keyStore)
  }
  val SSLTrustStore = getString("ssl-trust-store") match {
    case ""         ⇒ None
    case trustStore ⇒ Some(trustStore)
  }

  val SSLKeyStorePassword = getString("ssl-key-store-password") match {
    case ""       ⇒ None
    case password ⇒ Some(password)
  }

  val SSLTrustStorePassword = getString("ssl-trust-store-password") match {
    case ""       ⇒ None
    case password ⇒ Some(password)
  }

  val SSLSupportedAlgorithms = getStringList("ssl-supported-algorithms")

  val SSLProtocol = getString("ssl-protocol") match {
    case ""       ⇒ None
    case protocol ⇒ Some(protocol)
  }

  val EnableSSL = {
    val enableSSL = getBoolean("enable-ssl")
    if (enableSSL) {
      if (SSLProtocol.isEmpty) throw new ConfigurationException(
        "Configuration option 'akka.remote.netty.enable-ssl is turned on but no protocol is defined in 'akka.remote.netty.ssl-protocol'.")
      if (SSLKeyStore.isEmpty && SSLTrustStore.isEmpty) throw new ConfigurationException(
        "Configuration option 'akka.remote.netty.enable-ssl is turned on but no key/trust store is defined in 'akka.remote.netty.ssl-key-store' / 'akka.remote.netty.ssl-trust-store'.")
      if (SSLKeyStore.isDefined && SSLKeyStorePassword.isEmpty) throw new ConfigurationException(
        "Configuration option 'akka.remote.netty.ssl-key-store' is defined but no key-store password is defined in 'akka.remote.netty.ssl-key-store-password'.")
      if (SSLTrustStore.isDefined && SSLTrustStorePassword.isEmpty) throw new ConfigurationException(
        "Configuration option 'akka.remote.netty.ssl-trust-store' is defined but no trust-store password is defined in 'akka.remote.netty.ssl-trust-store-password'.")
    }
    enableSSL
  }
}