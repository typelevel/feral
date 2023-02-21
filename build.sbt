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

ThisBuild / tlBaseVersion := "0.2"
ThisBuild / startYear := Some(2021)

ThisBuild / developers := List(
  tlGitHubDev("armanbilge", "Arman Bilge"),
  tlGitHubDev("bpholt", "Brian Holt"),
  tlGitHubDev("djspiewak", "Daniel Spiewak")
)

ThisBuild / githubWorkflowJavaVersions := Seq("8", "11").map(JavaSpec.corretto(_))
ThisBuild / githubWorkflowBuildMatrixExclusions +=
  MatrixExclude(Map("project" -> "rootJS", "scala" -> Scala212))

ThisBuild / githubWorkflowBuild ~= { steps =>
  val scriptedStep = WorkflowStep.Sbt(
    List(s"scripted"),
    name = Some("Scripted"),
    cond = Some(s"matrix.scala == '$Scala212'")
  )
  steps.flatMap {
    case step @ WorkflowStep.Sbt(List("test"), _, _, _, _, _) =>
      val ciStep = step.copy(cond = Some(s"matrix.scala != '$Scala212'"))
      List(ciStep, scriptedStep)
    case step => List(step)
  }
}

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v2"),
    name = Some("Setup NodeJS v16 LTS"),
    params = Map("node-version" -> "16"),
    cond = Some("matrix.project == 'rootJS'")
  )

val Scala212 = "2.12.17"
val Scala213 = "2.13.10"
val Scala3 = "3.2.2"
ThisBuild / crossScalaVersions := Seq(Scala212, Scala3, Scala213)

val catsEffectVersion = "3.4.8"
val circeVersion = "0.14.4"
val fs2Version = "3.6.1"
val http4sVersion = "0.23.18"
val natchezVersion = "0.3.1"
val munitVersion = "0.7.29"
val munitCEVersion = "1.0.7"
val scalacheckEffectVersion = "1.0.4"

lazy val commonSettings = Seq(
  crossScalaVersions := Seq(Scala3, Scala213)
)

lazy val root =
  tlCrossRootProject.aggregate(
    core,
    lambda,
    sbtLambda,
    lambdaHttp4s,
    lambdaCloudFormationCustomResource,
    examples,
    unidocs
  )

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
      "io.circe" %%% "circe-jawn" % circeVersion,
      "org.scodec" %%% "scodec-bits" % "1.1.35",
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % munitCEVersion % Test,
      "io.circe" %%% "circe-literal" % circeVersion % Test
    )
  )
  .settings(commonSettings)
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-scalajs" % circeVersion,
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.2",
      "co.fs2" %%% "fs2-io" % fs2Version,
      "io.circe" %%% "circe-fs2" % "0.14.1"
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
    addSbtPlugin("io.chrisdavenport" %% "sbt-npm-package" % "0.1.2"),
    buildInfoPackage := "feral.lambda.sbt",
    buildInfoKeys += organization,
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++ Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scripted := scripted.dependsOn(core.js / publishLocal, lambda.js / publishLocal).evaluated
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
      "io.monix" %%% "newtypes-core" % "0.2.3",
      "org.http4s" %%% "http4s-client" % http4sVersion,
      "org.http4s" %%% "http4s-circe" % http4sVersion,
      "org.http4s" %%% "http4s-dsl" % http4sVersion % Test,
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % munitCEVersion % Test,
      "org.typelevel" %%% "scalacheck-effect" % scalacheckEffectVersion % Test,
      "org.typelevel" %%% "scalacheck-effect-munit" % scalacheckEffectVersion % Test,
      "com.eed3si9n.expecty" %%% "expecty" % "0.16.0" % Test,
      "io.circe" %%% "circe-testing" % circeVersion % Test
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
      "org.tpolecat" %%% "natchez-http4s" % "0.5.0",
      "org.tpolecat" %%% "skunk-core" % "0.5.1"
    )
  )
  .settings(commonSettings)
  .dependsOn(lambda, lambdaHttp4s)
  .enablePlugins(NoPublishPlugin)

lazy val unidocs = project
  .in(file("unidocs"))
  .enablePlugins(TypelevelUnidocPlugin)
  .settings(
    name := "feral-docs",
    ScalaUnidoc / unidoc / unidocProjectFilter := {
      if (scalaBinaryVersion.value == "2.12")
        inProjects(sbtLambda)
      else
        inProjects(
          core.jvm,
          lambda.jvm,
          lambdaHttp4s.jvm,
          lambdaCloudFormationCustomResource.jvm
        )
    }
  )
