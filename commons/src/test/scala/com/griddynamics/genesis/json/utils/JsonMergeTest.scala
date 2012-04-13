package com.griddynamics.genesis.json.utils

import org.junit.Test
import JsonMerge._
import io.Source
import net.liftweb.json.JsonParser
import junit.framework.Assert

class JsonMergeTest {
  val source = """
    {
    "test" : {
        "val1" : "%%foo%%",
        "val2" : "%%bar%%i%%baz%%",
        "val3" : "baz"
    }
}
    """
  val merged = JsonParser.parse("""{
    "test" : {
        "val1" : "fred",
        "val2" : "wilma",
        "val3" : "baz"
    }
    }""")

  val mergeStruct = JsonParser.parse("""{
    "test" : {
        "list" : [1, 2, 3]
    }
  }""")

  val afterMerge = JsonParser.parse("""{
        "test" : {
            "val1" : "%%foo%%",
            "val2" : "%%bar%%i%%baz%%",
            "val3" : "baz",
            "list" : [1, 2, 3]
        }
  }""")

  @Test
  def testMerge() {
    val merged_ = substituteByKey(Source.fromString(source),
      Map("val1" -> "fred", "val2" -> "wilma"))
    Assert.assertEquals(merged, merged_)
  }

  @Test
  def testMergeByName() {
    val merged_ = substituteByMask(Source.fromString(source),
      Map("foo" -> "fred", "bar" -> "w", "baz" -> "lma"))
    Assert.assertEquals(merged, merged_)
  }

    @Test
    def testFullMergeStructures() {
        val merged_ = merge(Source.fromString(source),
            mergeStruct)
        Assert.assertEquals(afterMerge, merged_)
    }
}
