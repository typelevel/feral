import _root_.io.circe.syntax._

scalaVersion := "2.13.8"
enablePlugins(LambdaJSPlugin)
scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule))
