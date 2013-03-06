package com.griddynamics.genesis.template

import org.apache.commons.vfs.{FileType, VFS, FileObject}
import java.net.{URL, URLClassLoader}
import java.util.jar.JarFile
import java.io.{FilenameFilter, File}
import org.apache.commons.io.FilenameUtils
import io.Source
import com.griddynamics.genesis.util.InputUtil

trait Storage[U] {
   def files: Iterable[U]
   def content(file: U): String
}

class FilesystemStorage(val path: String, val wildcard: String) extends Storage[File] {
  val genesisHome = Option(System.getProperty("genesis_home"))

  def files: Iterable[File] = {
    var topDir = new File(path)
    if (!topDir.exists() && genesisHome.isDefined) {
      val relativePath = new File(new File(genesisHome.get), path)
      if (relativePath.exists() && relativePath.isDirectory) {
        topDir = relativePath
      }
    }
    if (!topDir.exists || !topDir.isDirectory)
      throw new IllegalArgumentException("Given directory doesn't exist (%s)".format(topDir.getPath))
    topDir.listFiles(new FilenameFilter {
      def accept(dir: File, name: String) = FilenameUtils.wildcardMatch(name, wildcard, TemplateRepository.wildCardIOCase)
    })
  }

  def content(f: File): String = {
    Source.fromFile(f).getLines().mkString("\n")
  }
}

class ClasspathStorage(val classLoader : ClassLoader,
                       val wildcard : String,
                       val charset : String) extends Storage[FileObject] {
  import ClasspathStorage._
  val fileSelector = new WildcardFileSelector(wildcard)

  val dirs : Seq[FileObject] = classLoader match {
    case ucl : URLClassLoader => {
      ucl.getURLs.flatMap(url => {
        val file = vfsManager.resolveFile(url.toString)
        file.getName.getExtension.toLowerCase match {
          case "jar" => jarClassPath(file)
          case _ => Seq(file)
        }
      })
    }
    case _ => Seq()
  }

  def files = dirs.flatMap(_.findFiles(fileSelector))

  def content(file: FileObject) = {
    val content = file.getContent.getInputStream
    InputUtil.streamAsString(content, charset)
  }
}

object ClasspathStorage {
  val vfsManager = VFS.getManager

  def fileToJar(url : URL) = {
    val jarUrl = new URL("jar", url.getHost, url.getFile)
    vfsManager.resolveFile(jarUrl.toString)
  }

  def jarClassPath(url : URL) : Seq[String] = {
    try {
      val manifest = (new JarFile(new File(url.toURI))).getManifest
      val attributes = manifest.getMainAttributes
      Option(attributes.getValue("Class-Path")) match {
        case None => Seq()
        case Some(c) => c.split(" ")
      }
    }
    catch {
      case _: Throwable => Seq()
    }
  }

  def jarClassPath(file : FileObject) : Seq[FileObject] = {
    val url = file.getURL
    val parent = file.getParent
    val classPath = jarClassPath(url)
    val classPathFiles = classPath.map(cpe => parent.resolveFile(cpe))
      .filter(f => f.exists() && f.getType == FileType.FILE).map(_.getURL)
    (url +: classPathFiles).map(fileToJar(_))
  }
}
