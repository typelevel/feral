// give the user a nice default project!
ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.8"

lazy val root = (project in file("."))
  .enablePlugins(LambdaJSPlugin)
  .settings(
    name := "$name;format='norm'$",
    
    libraryDependencies = Seq(
      "org.typelevel" %%% "feral-lambda" % "0.1.0-M1",
      "org.typelevel" %%% "feral-lambda-http4s" % "0.1.0-M1",
      "org.typelevel" %%% "feral-lambda-cloudformation-custom-resource" % "0.1.0-M1"
    )
  )
