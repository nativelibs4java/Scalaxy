package plots

import java.io.File
import scala.io.Source
import scala.reflect.ClassTag
import scala.collection.mutable

object CSV {
  lazy val defaultSeparator = """\s*;\s*|\s+"""
  lazy val defaultCommentMark = "%"
  
  private def withFileSource[R](file: File, f: Source => R): R = {
    val s = Source.fromFile(file)
    try {
      f(s)
    } finally {
      s.close()
    }
  }
  def readCSV[A : ClassTag]
        (file: File, separator: String = defaultSeparator)
        (columns: String*)
        (f: PartialFunction[Array[String], A]) 
      : Seq[A] =
  {
    withFileSource(file, s => {
      readCSVFromSource(s, file.getName, separator)(columns: _*)(f)
    })
  }
  
  private[plots] // only exposed for testing
  def readCSVFromSource[A : ClassTag]
        (source: Source, sourceName: String, separator: String = defaultSeparator)
        (columns: String*)
        (f: PartialFunction[Array[String], A]) 
      : Seq[A] =
  {
    val out = mutable.ArrayBuffer[A]()
    var columnIndices: Array[Int] = null
    
    for ((rawLine, i) <- source.getLines.zipWithIndex; line = rawLine.trim; if !line.isEmpty) {
      val fields = line.split(separator)
      if (columnIndices eq null) {
        val indices = fields.zipWithIndex.toMap
        columnIndices = columns.toArray.map(c => indices.get(c).getOrElse {
          sys.error(s"""Column '$c' not found in $sourceName: found columns ${fields.map("'" + _ + "'").mkString(", ")}""")
        })
        //println(s"""Column indices for ${columns.mkString(", ")}: ${columnIndices.mkString(", ")}""")
      } else {
        try {
          val values = columnIndices.map(i => fields(i))
          out += f(values)
        } catch { case ex: Throwable =>
          sys.error(s"""Unexpected content at line ${i + 1} in $sourceName: "$line".""")
        }
      }
    }
    out.toSeq
  }
  
  def readFields[A : ClassTag]
        (file: File, separator: String = defaultSeparator, commentMark: String = defaultCommentMark)
        (f: PartialFunction[Array[String], A]) 
      : Seq[A] =
  {
    withFileSource(file, s => {
      readFieldsFromSource(s, file.getName, separator, commentMark)(f)
    })
  }
  
  private[plots] // only exposed for testing
  def readFieldsFromSource[A : ClassTag]
        (source: Source, sourceName: String, separator: String = defaultSeparator, commentMark: String = defaultCommentMark)
        (f: PartialFunction[Array[String], A]) 
      : Seq[A] =
  {
    val out = mutable.ArrayBuffer[A]()
    var columnIndices: Array[Int] = null
    
    for ((rawLine, i) <- source.getLines.zipWithIndex; line = rawLine.trim; if !line.isEmpty && !line.startsWith(commentMark)) {
      val fields = line.split(separator)
      if (!f.isDefinedAt(fields))
        sys.error(s"""Unexpected content at line ${i + 1} in $sourceName: "$line".""")
      
      out += f(fields)
    }
    out.toSeq
  }
}
