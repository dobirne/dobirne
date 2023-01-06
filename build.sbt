import BuildKeys._
import Boilerplate._

// ---------------------------------------------------------------------------
// Commands

addCommandAlias("ci-jvm", s";clean ;compile ;test")
addCommandAlias("ci-package", ";scalafmtCheckAll ;package")
addCommandAlias("ci", ";project root ;reload ;+scalafmtCheckAll ;+ci-jvm ;+package")

// ---------------------------------------------------------------------------
// Dependencies

val CatsVersion = "2.6.1"
val CatsEffectVersion = "3.2.7"
val ScalaTestVersion = "3.2.9"
val ScalaTestPlusVersion = "3.2.9.0"
val ScalaCheckVersion = "1.15.4"
val KindProjectorVersion = "0.13.2"
val BetterMonadicForVersion = "0.3.1"
val GitHub4sVersion = "0.29.1"

def defaultPlugins: Project ⇒ Project =
  pr => {
    val withCoverage = sys.env.getOrElse("SBT_PROFILE", "") match {
      case "coverage" => pr
      case _ => pr.disablePlugins(scoverage.ScoverageSbtPlugin)
    }

    withCoverage
  }

lazy val sharedSettings = Seq(
  projectTitle               := "dobirne",
  projectWebsiteRootURL      := "https://dobirne.github.io/",
  projectWebsiteBasePath     := "/dobirne/",
  githubOwnerID              := "dobirne",
  githubOwner                := "dobirne",
  githubRepository           := "dobirne",
  githubRelativeRepositoryID := "dobirne",
  organization               := "io.github.dobirne",
  scalaVersion               := "2.13.6",
  crossScalaVersions         := Seq("2.13.6" /*, "3.0.2"*/ ),
  // Turning off fatal warnings for doc generation
  Compile / doc / scalacOptions ~= filterConsoleScalacOptions,
  // Turning off fatal warnings and certain annoyances during testing
  Test / scalacOptions ~= (_ filterNot (Set(
    "-Xfatal-warnings",
    "-Werror",
    "-Ywarn-value-discard",
    "-Wvalue-discard"
  ))),
  // Compiler plugins that aren't necessarily compatible with Scala 3
  libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) =>
      Seq(
        compilerPlugin("com.olegpy" %% "better-monadic-for" % BetterMonadicForVersion),
        compilerPlugin("org.typelevel"                      % "kind-projector" % KindProjectorVersion cross CrossVersion.full)
      )
    case _ =>
      Seq.empty
  }),
  // ScalaDoc settings
  autoAPIMappings := true,
  scalacOptions ++= Seq(
    // Note, this is used by the doc-source-url feature to determine the
    // relative path of a given source file. If it's not a prefix of a the
    // absolute path of the source file, the absolute path of that file
    // will be put into the FILE_SOURCE variable, which is
    // definitely not what we want.
    "-sourcepath",
    file(".").getAbsolutePath.replaceAll("[.]$", "")
  ),
  // https://github.com/sbt/sbt/issues/2654
  incOptions := incOptions.value.withLogRecompileOnMacro(false),
  // ---------------------------------------------------------------------------
  // Options for testing
  Test / logBuffered            := false,
  IntegrationTest / logBuffered := false,
  // ---------------------------------------------------------------------------
  // Options meant for publishing on Maven Central
  Test / publishArtifact := false,
  pomIncludeRepository   := { _ => false }, // removes optional dependencies
  licenses               := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage               := Some(url(projectWebsiteFullURL.value)),
  scmInfo := Some(
    ScmInfo(
      url(s"https://github.com/${githubFullRepositoryID.value}"),
      s"scm:git@github.com:${githubFullRepositoryID.value}.git"
    )),
  developers := List(
    Developer(
      id = "zakolenko",
      name = "Roman Zakolenko",
      email = "zakolenkoroman@gmail.com",
      url = url("https://zakolenko.github.io")
    ))
)

/**
  * Shared configuration across all sub-projects with actual code to be published.
  */
def defaultProjectConfiguration(pr: Project): Project = {
  pr.configure(defaultPlugins)
    .settings(sharedSettings)
    .settings(crossVersionSharedSources)
    .settings(
      filterOutMultipleDependenciesFromGeneratedPomXml(
        "groupId" -> "org.scoverage".r :: Nil
      ))
}

lazy val root = project
  .in(file("."))
  .aggregate(
    `redis4cats-contrib-core`,
    `redis4cats-contrib-bench`,
    `tapir-contrib-server`,
    `tapir-contrib-log4cats`,
    `fs2-contrib-batcher`,
    `phantom-id`
  )
  .configure(defaultPlugins)
  .settings(sharedSettings)
  .settings(doNotPublishArtifact)
  .settings(
    // Try really hard to not execute tasks in parallel ffs
    Global / concurrentRestrictions := Tags.limitAll(1) :: Nil,
    // Reloads build.sbt changes whenever detected
    Global / onChangedBuildSource := ReloadOnSourceChanges,
    // Deactivate sbt's linter for some temporarily unused keys
    Global / excludeLintKeys ++= Set(
      IntegrationTest / logBuffered,
      coverageExcludedFiles,
      githubRelativeRepositoryID
    )
  )

lazy val `redis4cats-contrib-core` = {
  project
    .configure(redis4catsModule("core"))
    .configure(defaultProjectConfiguration)
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core"                 % CatsVersion,
        "org.typelevel" %% "cats-effect"               % CatsEffectVersion,
        "dev.profunktor" %% "redis4cats-effects"       % "1.0.0",
        "com.dimafeng" %% "testcontainers-scala-munit" % "0.40.11" % Test,
        "org.typelevel" %% "munit-cats-effect-3"       % "1.0.7"   % Test
      )
    )
}

lazy val `redis4cats-contrib-bench` = {
  project
    .configure(redis4catsModule("bench"))
    .enablePlugins(JmhPlugin)
    .configure(defaultProjectConfiguration)
    .dependsOn(`redis4cats-contrib-core`)
}

lazy val `tapir-contrib-server` = {
  project
    .configure(tapirModule("server"))
    .configure(defaultProjectConfiguration)
    .settings(
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.tapir" %% "tapir-server" % "1.2.1"
      )
    )
}

lazy val `tapir-contrib-log4cats` = {
  project
    .configure(tapirModule("log4cats"))
    .configure(defaultProjectConfiguration)
    .dependsOn(`tapir-contrib-server`)
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel" %% "log4cats-core" % "2.5.0"
      )
    )
}

lazy val `phantom-id` = {
  project
    .configure(dobirneModule("phantom-id"))
    .configure(defaultProjectConfiguration)
}



lazy val `fs2-contrib-batcher` = {
  project
    .configure(fs2Module("batcher"))
    .configure(defaultProjectConfiguration)
    .settings(
      libraryDependencies ++= Seq(
        "co.fs2" %% "fs2-core" % "3.0.4"
      )
    )
}

def submodule(module: String, submodule: String): Project => Project = { project =>
  project
    .in(file(s"$module/$submodule"))
    .settings(moduleName := s"$module-$submodule")
}

def redis4catsModule(submoduleName: String): Project => Project = submodule("redis4cats-contrib", submoduleName)
def tapirModule(submoduleName: String): Project => Project = submodule("tapir-contrib", submoduleName)

def fs2Module(submoduleName: String): Project => Project = submodule("fs2-contrib", submoduleName)
def dobirneModule(submoduleName: String): Project => Project = submodule("dobirne", submoduleName)
