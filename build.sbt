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

import com.typesafe.tools.mima.core._

name := "feral"

ThisBuild / tlBaseVersion := "0.3"
ThisBuild / startYear := Some(2021)

ThisBuild / developers := List(
  tlGitHubDev("armanbilge", "Arman Bilge"),
  tlGitHubDev("bpholt", "Brian Holt"),
  tlGitHubDev("djspiewak", "Daniel Spiewak")
)

ThisBuild / githubWorkflowJavaVersions := Seq("8", "11", "17").map(JavaSpec.corretto(_))

ThisBuild / githubWorkflowBuildMatrixExclusions ++=
  List("rootJS", "rootJVM").map(p => MatrixExclude(Map("project" -> p, "scala" -> "2.12"))) ++
    List("2.13", "3").map(s => MatrixExclude(Map("project" -> "rootSbtScalafix", "scala" -> s)))

ThisBuild / githubWorkflowBuildMatrixAdditions ~= { matrix =>
  matrix + ("project" -> (matrix("project") :+ "rootSbtScalafix"))
}

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v4"),
    name = Some("Setup NodeJS v20 LTS"),
    params = Map("node-version" -> "20"),
    cond = Some("matrix.project == 'rootJS'")
  )

val Scala212 = "2.12.19"
val Scala213 = "2.13.14"
val Scala3 = "3.3.3"
ThisBuild / crossScalaVersions := Seq(Scala212, Scala3, Scala213)

val catsEffectVersion = "3.5.4"
val circeVersion = "0.14.7"
val fs2Version = "3.10.2"
val http4sVersion = "0.23.27-10-fa6e976-SNAPSHOT"
val natchezVersion = "0.3.5"
val munitVersion = "0.7.29"
val munitCEVersion = "1.0.7"
val scalacheckEffectVersion = "1.0.4"
ThisBuild / resolvers += "s01-oss-sonatype-org-snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots"

lazy val commonSettings = Seq(
  crossScalaVersions := Seq(Scala3, Scala213)
)

lazy val root =
  tlCrossRootProject
    .aggregate(
      lambda,
      lambdaHttp4s,
      lambdaCloudFormationCustomResource,
      vercelNodeJS,
      examples,
      unidocs
    )
    .configureRoot(
      _.aggregate(sbtLambda).aggregate(scalafix.componentProjectReferences: _*)
    )

lazy val rootSbtScalafix = project
  .in(file(".rootSbtScalafix"))
  .aggregate(sbtLambda)
  .aggregate(scalafix.componentProjectReferences: _*)
  .enablePlugins(NoPublishPlugin)

lazy val lambda = crossProject(JSPlatform, JVMPlatform)
  .in(file("lambda"))
  .settings(
    name := "feral-lambda",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectVersion,
      "org.tpolecat" %%% "natchez-core" % natchezVersion,
      "io.circe" %%% "circe-scodec" % circeVersion,
      "io.circe" %%% "circe-jawn" % circeVersion,
      "com.comcast" %%% "ip4s-core" % "3.6.0",
      "org.scodec" %%% "scodec-bits" % "1.2.0",
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % munitCEVersion % Test,
      "io.circe" %%% "circe-literal" % circeVersion % Test
    ),
    mimaBinaryIssueFilters ++= Seq(
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("feral.lambda.IOLambda.setupMemo")
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
    Test / fork := true,
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.3",
      "co.fs2" %%% "fs2-io" % fs2Version
    )
  )

lazy val sbtLambda = project
  .in(file("sbt-lambda"))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .settings(
    name := "sbt-feral-lambda",
    crossScalaVersions := Seq(Scala212),
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
    addSbtPlugin("io.chrisdavenport" %% "sbt-npm-package" % "0.2.0"),
    buildInfoPackage := "feral.lambda.sbt",
    buildInfoKeys += organization,
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++ Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scripted := scripted.dependsOn(lambda.js / publishLocal).evaluated,
    Test / test := scripted.toTask("").value
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

lazy val vercelNodeJS = project
  .in(file("vercel-nodejs"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "feral-vercel-nodejs",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectVersion,
      "org.http4s" %%% "http4s-core" % http4sVersion
    ),
    tlVersionIntroduced := List("2.13", "3").map(_ -> "0.3.1").toMap
  )

lazy val examples = crossProject(JSPlatform, JVMPlatform)
  .in(file("examples"))
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-dsl" % http4sVersion,
      "org.http4s" %%% "http4s-ember-client" % http4sVersion,
      "org.tpolecat" %%% "natchez-xray" % natchezVersion,
      "org.tpolecat" %%% "natchez-http4s" % "0.5.0",
      "org.tpolecat" %%% "skunk-core" % "0.6.4"
    )
  )
  .settings(commonSettings)
  .dependsOn(lambda, lambdaHttp4s)
  .jsConfigure(_.dependsOn(vercelNodeJS))
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass := Some("feral.examples.vercelNodejsHandler"),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )
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
          lambda.jvm,
          lambdaHttp4s.jvm,
          lambdaCloudFormationCustomResource.jvm
        )
    }
  )

lazy val scalafix = tlScalafixProject
  .rulesSettings(
    name := "feral-scalafix",
    startYear := Some(2023),
    crossScalaVersions := Seq(Scala212)
  )
  .inputSettings(
    crossScalaVersions := Seq(Scala213),
    libraryDependencies += "org.typelevel" %%% "feral-lambda-http4s" % "0.2.4",
    headerSources / excludeFilter := AllPassFilter
  )
  .inputConfigure(_.disablePlugins(ScalafixPlugin))
  .outputSettings(
    crossScalaVersions := Seq(Scala213),
    headerSources / excludeFilter := AllPassFilter
  )
  .outputConfigure(_.dependsOn(lambdaHttp4s.jvm).disablePlugins(ScalafixPlugin))
  .testsSettings(
    startYear := Some(2023),
    crossScalaVersions := Seq(Scala212)
  )
