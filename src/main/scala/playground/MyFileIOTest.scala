package playground

import java.io.File

import akka.stream.scaladsl.FileIO

object MyFileIOTest extends App {
  println("MyFileIOTest")

  val filename = "src/main/resources/download/temp"
  val file = new File(filename)
  FileIO.toPath(file.toPath)

}
