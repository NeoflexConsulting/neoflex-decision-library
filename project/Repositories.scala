import sbt.Keys.isSnapshot
import sbt._

object Repositories {
  private val NexusUrl = "http://nexus.do.neoflex.ru:8081/repository/"

  val MavenPublic = ("maven-public" at s"$NexusUrl/maven-public/").withAllowInsecureProtocol(true)
  val MavenReleases = ("maven-releases" at s"$NexusUrl/maven-releases/").withAllowInsecureProtocol(true)
  val MavenSnapshots = ("maven-snapshots" at s"$NexusUrl/maven-snapshots/").withAllowInsecureProtocol(true)

  def resolvers: Seq[Resolver] = Seq(Resolver.mavenLocal, MavenPublic)

  def publishToRepository = Def.task {
    if (isSnapshot.value)
      Some(MavenSnapshots)
    else
      Some(MavenReleases)
  }
}
