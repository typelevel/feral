ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := $if(scala3.truthy)$"3.0.0"$else$"2.13.5"$endif$

val natchezVersion = "0.1.6"
lazy val root = (project in file("."))
  .enablePlugins(LambdaJSPlugin)
  .settings(
    name := "$name;format="norm"$",
    
    libraryDependencies ++= Seq(
      // JVM setup
      "org.typelevel" %% "feral-lambda" % "0.1.0-M1",

      // Optional, specialized integrations, available for both JS and JVM
      "org.typelevel" %%% "feral-lambda-http4s" % "0.1.0-M1",
      "org.typelevel" %%% "feral-lambda-cloudformation-custom-resource" % "0.1.0-M1",

      // For the example
      "org.tpolecat" %%% "natchez-xray" % natchezVersion,
      "org.tpolecat" %%% "natchez-http4s" % "0.3.2",
      "org.tpolecat" %%% "skunk-core" % "0.3.2"
    ),
  )
