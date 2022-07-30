import cats.effect._
import feral.lambda._
import scala.scalajs.js.annotation._

object mySimpleHandler extends IOLambda.Simple[Unit, INothing] {
  def apply(event: Unit, context: Context[IO], init: Init): IO[Option[INothing]] = IO.none

  @JSExportTopLevel("exportedHandler")
  val impl: HandlerFn = handlerFn
}
