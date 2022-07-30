import _root_.io.circe.syntax._

scalaVersion := "2.13.8"
enablePlugins(LambdaJSPlugin)
scalaJSUseMainModuleInitializer := false
scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule))
npmPackageAdditionalNpmConfig := Map("type" -> "module".asJson)
