resolvers += "s01" at "https://s01.oss.sonatype.org/content/repositories/snapshots/"
val sbtlTlV = "0.6.2-10-db40ea3-SNAPSHOT"
addSbtPlugin("org.typelevel" % "sbt-typelevel" % sbtlTlV)
addSbtPlugin("org.typelevel" % "sbt-typelevel-scalafix" % sbtlTlV)
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.14.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
