/*
 * Copyright 2021 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

name := "feral"

ThisBuild / baseVersion := "0.1"

ThisBuild / organization := "org.typelevel"
ThisBuild / organizationName := "Typelevel"

ThisBuild / developers := List(
  Developer("armanbilge", "Arman Bilge", "@armanbilge", url("https://github.com/armanbilge")),
  Developer("bpholt", "Brian Holt", "@bpholt", url("https://github.com/bpholt")),
  Developer("djspiewak", "Daniel Spiewak", "@djspiewak", url("https://github.com/djspiewak"))
)

enablePlugins(SonatypeCiReleasePlugin)
ThisBuild / spiewakCiReleaseSnapshots := true
ThisBuild / spiewakMainBranches := Seq("main")
ThisBuild / homepage := Some(url("https://github.com/typelevel/feral"))
ThisBuild / scmInfo := Some(
  ScmInfo(url("https://github.com/typelevel/feral"), "git@github.com:typelevel/feral.git"))

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"), JavaSpec.temurin("11"))
ThisBuild / githubWorkflowBuildMatrixExclusions ++= {
  for {
    scala <- (ThisBuild / crossScalaVersions).value.init
    java <- (ThisBuild / githubWorkflowJavaVersions).value.tail
  } yield MatrixExclude(Map("scala" -> scala, "java" -> java.render))
}

ThisBuild / githubWorkflowGeneratedUploadSteps ~= { steps =>
  val mkdirStep = steps.headOption match {
    case Some(WorkflowStep.Run(command :: _, _, _, _, _, _)) =>
      WorkflowStep.Run(
        commands = List(command.replace("tar cf targets.tar", "mkdir -p")),
        name = Some("Make target directories")
      )
    case _ => sys.error("Can't generate make target dirs workflow step")
  }
  mkdirStep +: steps
}

ThisBuild / githubWorkflowBuildPreamble += WorkflowStep.Sbt(
  List(s"++$Scala213", "publishLocal"),
  name = Some("Publish local"),
  cond = Some(s"matrix.scala == '$Scala212'")
)

ThisBuild / githubWorkflowBuild ~= { steps =>
  val ciStep = steps.headOption match {
    case Some(step @ WorkflowStep.Sbt(_, _, _, _, _, _)) =>
      step.copy(cond = Some(s"matrix.scala != '$Scala212'"))
    case _ => sys.error("Can't generate ci step")
  }
  val scriptedStep = WorkflowStep.Sbt(
    List(s"scripted"),
    name = Some("Run sbt scripted tests"),
    cond = Some(s"matrix.scala == '$Scala212'")
  )
  List(ciStep, scriptedStep)
}

replaceCommandAlias(
  "ci",
  "; project /; headerCheckAll; scalafmtCheckAll; scalafmtSbtCheck; clean; testIfRelevant; mimaReportBinaryIssuesIfRelevant"
)

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v2"),
    name = Some("Setup NodeJS v14 LTS"),
    params = Map("node-version" -> "14")
  )

val Scala212 = "2.12.15"
val Scala213 = "2.13.7"
val Scala3 = "3.1.0"
ThisBuild / crossScalaVersions := Seq(Scala212, Scala3, Scala213)

val catsEffectVersion = "3.3.3"
val circeVersion = "0.14.1"
val fs2Version = "3.2.4"
val http4sVersion = "0.23.7"
val natchezVersion = "0.1.6"
val munitVersion = "0.7.29"
val munitCEVersion = "1.0.7"

lazy val commonSettings = Seq(
  crossScalaVersions := Seq(Scala3, Scala213),
  scalacOptions ++= {
    if (isDotty.value && githubIsWorkflowBuild.value)
      Seq("-Xfatal-warnings")
    else
      Seq.empty
  }
)

lazy val root =
  project
    .in(file("."))
    .aggregate(
      core.js,
      core.jvm,
      lambda.js,
      lambda.jvm,
      sbtLambda,
      lambdaHttp4s.js,
      lambdaHttp4s.jvm,
      lambdaCloudFormationCustomResource.js,
      lambdaCloudFormationCustomResource.jvm,
      examples.js,
      examples.jvm
    )
    .enablePlugins(NoPublishPlugin)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "feral-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectVersion
    )
  )
  .settings(commonSettings)

lazy val lambda = crossProject(JSPlatform, JVMPlatform)
  .in(file("lambda"))
  .settings(
    name := "feral-lambda",
    libraryDependencies ++= Seq(
      "org.tpolecat" %%% "natchez-core" % natchezVersion,
      "io.circe" %%% "circe-scodec" % circeVersion,
      "org.scodec" %%% "scodec-bits" % "1.1.30",
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % "1.0.7" % Test
    ),
    libraryDependencies ++= {
      if (isDotty.value) Nil
      else
        Seq(
          "io.circe" %%% "circe-literal" % circeVersion % Test,
          "io.circe" %% "circe-jawn" % circeVersion % Test // %% b/c used for literal macro at compile-time only
        )
    }
  )
  .settings(commonSettings)
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-scalajs" % circeVersion,
      "io.github.cquiroz" %%% "scala-java-time" % "2.3.0"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
      "co.fs2" %%% "fs2-io" % fs2Version,
      "io.circe" %%% "circe-jawn" % circeVersion,
      "io.circe" %%% "circe-fs2" % "0.14.0"
    )
  )
  .dependsOn(core)

lazy val sbtLambda = project
  .in(file("sbt-lambda"))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .settings(
    name := "sbt-feral-lambda",
    crossScalaVersions := Seq(Scala212),
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
    addSbtPlugin("io.chrisdavenport" %% "sbt-npm-package" % "0.1.0"),
    buildInfoPackage := "feral.lambda.sbt",
    buildInfoKeys += organization,
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++ Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    }
  )

lazy val lambdaHttp4s = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("lambda-http4s"))
  .settings(
    name := "feral-lambda-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-server" % http4sVersion
    )
  )
  .settings(commonSettings)
  .dependsOn(lambda % "compile->compile;test->test")

lazy val lambdaCloudFormationCustomResource = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("lambda-cloudformation-custom-resource"))
  .settings(
    name := "feral-lambda-cloudformation-custom-resource",
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-Ywarn-macros:after")
      case _ => Nil
    }),
    libraryDependencies ++= Seq(
      "io.monix" %%% "newtypes-core" % "0.0.1",
      "org.http4s" %%% "http4s-client" % http4sVersion,
      "org.http4s" %%% "http4s-circe" % http4sVersion
    )
  )
  .settings(commonSettings)
  .dependsOn(lambda)

lazy val examples = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("examples"))
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-dsl" % http4sVersion,
      "org.http4s" %%% "http4s-ember-client" % http4sVersion,
      "org.tpolecat" %%% "natchez-xray" % natchezVersion,
      "org.tpolecat" %%% "natchez-http4s" % "0.2.1",
      "org.tpolecat" %%% "skunk-core" % "0.2.3"
    )
  )
  .settings(commonSettings)
  .dependsOn(lambda, lambdaHttp4s)
  .enablePlugins(NoPublishPlugin)
