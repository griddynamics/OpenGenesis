package com.griddynamics.genesis.resources

import javax.servlet._
import http.{HttpServletResponse, HttpServletRequest}
import org.apache.commons.vfs.{FileType, FileContent, VFS, FileObject}
import util.parsing.json.{JSONObject, JSONArray, JSONType, JSON}
import io.Source
import com.griddynamics.genesis.util.Logging
import java.io.{OutputStream, ByteArrayInputStream, InputStream}

class ContentFilter extends Filter with Logging {

  val manager = VFS.getManager
  var mappings: Map[String, MappingDescriptor] = Map()
  var config: FilterConfig = _
  var filterPath: String = _

  def addRoots(nodes: List[Any]) = {
    nodes.map {case m : Map[String, String]  => (m("mapping"), MappingDescriptor(m("path"), m("mapping"), m.get("link"), m.get("index")))}.toMap
  }

  def configure(s: String) {
    val file: FileObject = manager.resolveFile(s)
    if (file.exists() && file.isReadable) {
      val content: FileContent = file.getContent
      val configContent = JSON.parseFull(Source.fromInputStream(content.getInputStream).mkString)
      val configuredMappings = configContent.map({
        case json: List[Any] => addRoots(json)
      })
      configuredMappings.foreach( mappings = _ )
    }
  }

  def init(filterConfig: FilterConfig) {
    val jsonPath: String = filterConfig.getInitParameter(ContentFilter.InitName)
    filterPath = filterConfig.getInitParameter(ContentFilter.Mapping)
    config = filterConfig
    configure(jsonPath)
    log.debug(s"Configured mappings: ${mappings}")
  }

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val req = request.asInstanceOf[HttpServletRequest]
    val resp = response.asInstanceOf[HttpServletResponse]
    val content = resolve(req.getServletPath)
    if (content.isDefined) {
      val (filename, stream) = content.get
      val contentType = Option(config.getServletContext.getMimeType(filename))
      resp.setHeader("Content-type", contentType.getOrElse("application/octet-stream"))
      val out: OutputStream = response.getOutputStream
      ResourceFilter.copyStream(stream, out)
      out.flush()
      out.close()
      stream.close()
    } else {
      chain.doFilter(request, response)
    }
  }

  def resolveFile(descriptor: MappingDescriptor, path: String): Option[FileObject] = {
    val fullPath = s"${descriptor.path}/${path}"
    val file: FileObject = manager.resolveFile(fullPath)
    if (file.exists() && file.isReadable && file.getType == FileType.FILE)
      Some(file)
    else
      None
  }

  def resolve(path: String) : Option[(String, InputStream)] = {
    val components: Array[String] = path.split("/").dropWhile(s => s.isEmpty).tail
    if (components.head == "index.json") {
      val idx = JSONArray(index).toString().getBytes
      Some(components.head, new ByteArrayInputStream(idx))
    } else {
      mappings.get(components.head) flatMap {
        mapping => {
          resolveFile(mapping, components.tail.mkString("/")) map {file => (file.getName.getBaseName, file.getContent.getInputStream)}
        }
      }
    }
  }

  def index = mappings.values.collect({case MappingDescriptor(_, mapping, link, index)
    if index.isDefined && link.isDefined => JSONObject(Map("link" -> link.get, "index" -> s"${filterPath}/${mapping}/${index.get}"))}).toList

  def destroy() {}
}

object ContentFilter {
  val InitName = "content.json"
  val Mapping = "mapping"
}

case class MappingDescriptor(path: String, mapping: String, link: Option[String], index: Option[String])
