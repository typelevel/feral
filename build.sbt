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
val http4sVersion = "0.23.27"
val natchezVersion = "0.3.5"
val munitVersion = "0.7.29"
val munitCEVersion = "1.0.7"
val scalacheckEffectVersion = "1.0.4"

lazy val commonSettings = Seq(
  crossScalaVersions := Seq(Scala3, Scala213)
)

lazy val root =
  tlCrossRootProject
    .aggregate(
      `lambda-kernel`,
      `lambda-runtime-binding`,
      `lambda-runtime`,
      lambdaHttp4s,
      lambdaCloudFormationCustomResource,
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

lazy val `lambda-kernel` = crossProject(JSPlatform, JVMPlatform)
  .in(file("lambda-kernel"))
  .settings(
    name := "feral-lambda-kernel",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectVersion,
      "org.tpolecat" %%% "natchez-core" % natchezVersion,
      "io.circe" %%% "circe-scodec" % circeVersion,
      "io.circe" %%% "circe-jawn" % circeVersion,
      "com.comcast" %%% "ip4s-core" % "3.6.0",
      "org.scodec" %%% "scodec-bits" % "1.2.0",
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test,
      "io.circe" %%% "circe-literal" % circeVersion % Test
    ),
    mimaPreviousArtifacts := Set(
      "org.typelevel" %%% "feral-lambda" % "0.3.0"
    ),
    mimaBinaryIssueFilters ++= Seq(
      // These classes are moved to lambda-runtime-binding module
      ProblemFilters.exclude[MissingTypesProblem]("feral.lambda.Context$"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.ContextCompanionPlatform"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.IOLambda"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.IOLambda$"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.IOLambda$Simple"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.IOLambdaPlatform"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("feral.lambda.Context.xRayTraceId"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("feral.lambda.Context#Impl.copy"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("feral.lambda.Context#Impl.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("feral.lambda.Context#Impl.this")
    )
  )
  .settings(commonSettings)
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.5.0",
    mimaBinaryIssueFilters ++= Seq(
      // These classes are moved to lambda-runtime-binding module
      ProblemFilters.exclude[DirectMissingMethodProblem]("feral.lambda.Context.fromJS"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.facade.ClientContext"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.facade.ClientContextClient"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.facade.ClientContextEnv"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.facade.CognitoIdentity"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.facade.Context")
    )
  )
  .jvmSettings(
    mimaBinaryIssueFilters ++= Seq(
      // These classes are moved to lambda-runtime-binding module
      ProblemFilters.exclude[DirectMissingMethodProblem]("feral.lambda.Context.fromJava")
    )
  )

lazy val `lambda-runtime` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("lambda-runtime"))
  .settings(
    name := "feral-lambda-runtime",
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-core" % http4sVersion,
      "org.http4s" %%% "http4s-ember-client" % http4sVersion,
      "org.http4s" %%% "http4s-circe" % http4sVersion,
      "org.http4s" %%% "http4s-dsl" % http4sVersion % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % munitCEVersion % Test,
      "io.circe" %%% "circe-literal" % circeVersion % Test
    ),
    mimaPreviousArtifacts := Set.empty
  )
  .settings(commonSettings)
  .dependsOn(`lambda-kernel`)

lazy val `lambda-runtime-binding` = crossProject(JSPlatform, JVMPlatform)
  .in(file("lambda-runtime-binding"))
  .settings(
    name := "feral-lambda-runtime-binding",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "munit-cats-effect-3" % munitCEVersion % Test,
      "io.circe" %%% "circe-literal" % circeVersion % Test
    ),
    mimaPreviousArtifacts := Set(
      "org.typelevel" %%% "feral-lambda" % "0.3.0"
    ),
    mimaBinaryIssueFilters ++= Seq(
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("feral.lambda.IOLambda.setupMemo"),
      ProblemFilters.exclude[MissingClassProblem]("feral.lambda.*")
    )
  )
  .settings(commonSettings)
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-scalajs" % circeVersion
    )
  )
  .jvmSettings(
    Test / fork := true,
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.3",
      "co.fs2" %%% "fs2-io" % fs2Version
    )
  )
  .dependsOn(`lambda-kernel`)

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
    scripted := scripted
      .dependsOn(`lambda-kernel`.js / publishLocal, `lambda-runtime-binding`.js / publishLocal)
      .evaluated,
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
  .dependsOn(
    `lambda-kernel` % "compile->compile;test->test",
    `lambda-runtime-binding` % "compile->compile;test->test")

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
  .dependsOn(`lambda-runtime-binding`)

lazy val examples = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
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
  .dependsOn(`lambda-runtime-binding`, lambdaHttp4s)
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
          `lambda-kernel`.jvm,
          `lambda-runtime-binding`.jvm,
          `lambda-runtime`.jvm,
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
