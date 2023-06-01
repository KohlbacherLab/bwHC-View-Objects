


name := "mtb-views"
ThisBuild / organization := "de.bwhc"
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "1.0-SNAPSHOT"


//-----------------------------------------------------------------------------
// PROJECT
//-----------------------------------------------------------------------------

lazy val root = project.in(file("."))
  .settings(settings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest"  %% "scalatest"               % "3.1.1" % Test,
      "de.bwhc"        %% "utils"                   % "1.1",
      "de.bwhc"        %% "mtb-dtos"                % "1.0-SNAPSHOT",
      "de.bwhc"        %% "mtb-dto-extensions"      % "1.0-SNAPSHOT",
      "de.bwhc"        %% "mtb-dto-generators"      % "1.0-SNAPSHOT" % Test,
      "de.bwhc"        %% "hgnc-impl"               % "1.0" % Test,
      "de.bwhc"        %% "icd-catalogs-impl"       % "1.0" % Test,
      "de.bwhc"        %% "medication-catalog-impl" % "1.0" % Test,
   )
 )


//-----------------------------------------------------------------------------
// SETTINGS
//-----------------------------------------------------------------------------

lazy val settings = commonSettings

lazy val compilerOptions = Seq(
  "-encoding", "utf8",
  "-unchecked",
  "-Xfatal-warnings",
  "-feature",
  "-language:higherKinds",
  "-language:postfixOps",
  "-deprecation"
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++=
    Seq("Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository") ++
    Resolver.sonatypeOssRepos("releases") ++
    Resolver.sonatypeOssRepos("snapshots")
)

