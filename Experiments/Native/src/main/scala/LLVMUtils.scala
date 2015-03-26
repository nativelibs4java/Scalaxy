package scalaxy.native

import java.io._

import scala.collection.mutable.ArrayBuffer

private[native] trait LLVMUtils extends IOUtils
{
  private[this] lazy val messageRx = "^(.*?):(\\d+):(\\d+): (fatal error|error|warning|info): (.*)".r

  case class CLangMessage(file: String, tpe: String, text: String, column: Int, line: Int)

  object clang {
    val clangBin = Option(System.getenv("clang")).getOrElse(System.getProperty("clang", "clang"))
    ///usr/bin/clang"))
    def apply(args: String*) = {
      import scala.sys.process._

      val stdLines = new ArrayBuffer[String]()
      val errLines = new ArrayBuffer[String]()
      val logger = ProcessLogger(stdLines += _, errLines += _)
      
      // clang-check -analyze -extra-arg -Xclang -extra-arg -analyzer-output=text
      val cmd = clangBin :: args.toList
      // println("cmd = " + cmd)
      cmd ! logger

      (stdLines.toList, errLines.toList)
    }
  }

  def compileCSource(src: String, includes: List[String]): (Option[String], List[CLangMessage]) = {
    // import java.io._
    val prefix = "scalaxy-native."
    val suffix = ".cpp"
    withTempFile(prefix, suffix, inputFile => {
      val inputFileAbsolutePath = inputFile.getAbsolutePath
      withTempFile(prefix, suffix, outputFile => {
        writeFile(inputFile, src)

        val args =
          List(
            "-Weverything",
            "-Wno-missing-prototypes",
            "-Wno-c99-extensions",
            "-O3",
            // "-Wno-unused-parameter",
            "-S",
            "-emit-llvm",
            inputFile.getPath,
            "-o", outputFile.getPath) ++
          includes.map("-I" + _) 

        val (stdout, stderr) = clang(args: _*)

        println(src)
        // println(stdout)
        // println(stderr.mkString("\n"))

        val msgs = (stderr collect {
          case msg @ messageRx(file, line, column, tpe, text) =>
            if (file == inputFileAbsolutePath) {
              Some(CLangMessage(file, tpe, text, line = line.toInt, column = column.toInt))
            } else {
              println(msg)
              None
            }
          // case line if line.contains(":") =>
          //   sys.error("Failed to parse: " + line)
        }).flatten

        (if (outputFile.exists) Some(readFile(outputFile)) else None) -> msgs
      })
    })
  }
}
