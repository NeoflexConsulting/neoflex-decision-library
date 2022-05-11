import sbt.Keys._
import sbt.file
import sbtrelease.ReleasePlugin.autoImport.{
  releaseCommitMessage,
  releaseIgnoreUntrackedFiles,
  releaseNextCommitMessage,
  releaseTagComment,
  releaseTagName,
  releaseUseGlobalVersion,
  releaseVersionBump,
  releaseVersionFile
}
import sbtrelease.Version.Bump.Minor

object Settings {

  def artifactSettings(baseDir: String) =
    Seq(
      publish / skip := false,
      publishMavenStyle := true,
      publishTo := Repositories.publishToRepository.value
    ) ++ releaseSettings(baseDir)

  def releaseSettings(baseDir: String) = Seq(
    releaseUseGlobalVersion := false,
    releaseVersionFile := file(s"$baseDir/version.sbt"),
    releaseTagName := s"${name.value}-${version.value}",
    releaseTagComment := s"Releasing ${name.value}-${version.value}",
    releaseCommitMessage := s"Bump version ${name.value}-${version.value}",
    releaseNextCommitMessage := s"Bump version ${name.value}-${version.value}",
    releaseVersionBump := Minor,
    releaseIgnoreUntrackedFiles := true
  )
}
