package com.griddynamics.genesis.json.utils

import scala.io.Source
import net.liftweb.json.JsonAST.{JValue, JString, JField}
import net.liftweb.json.{JsonParser, parse, render, compact}

object JsonMerge {
  def substituteByKey(source : Source, replacements: Map[String, String]) =
    parse(source.getLines().mkString) transform {
      case JField(name, v) =>
        replacements.get(name) match {
          case Some(s) =>
            JField(name, JString(s))
          case None => JField(name, v)
        }
    }
  
  def substituteByMask(source : Source,  replacements : Map[String, String]) =
    parse(source.getLines().mkString) transform {
      case JField(name, JString(v)) => {
        val start = v.indexOf("%%")
        start match {
          case -1 => JField(name, JString(v))
          case i => {
            val map = replacements.foldLeft(v)((s, pair2) => s.replaceAll("%%" + pair2._1 + "%%", pair2._2))
            JField(name, JString(map))
          }
        }
      }
    }

    def merge(source: JValue, attributes: JValue) : JValue = {
        source.merge(attributes)
    }
  
    implicit def JValueToSource(value: JValue) : Source = Source.fromString(compact(render(value)))
    implicit def SourceToJValue(value: Source) : JValue = JsonParser.parse(value.getLines().mkString)

}
