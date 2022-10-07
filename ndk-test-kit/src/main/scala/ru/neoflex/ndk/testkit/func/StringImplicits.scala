package ru.neoflex.ndk.testkit.func

trait StringImplicits {
  implicit class StringAsPath(s: String) {
    def resourcePath: String = getClass.getResource(s"/$s").getFile

    def inTmpDir: String = s"${scala.util.Properties.tmpDir}/$s"
  }
}
