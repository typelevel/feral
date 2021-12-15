import cats.effect._
import feral.lambda._

object mySimpleHandler extends IOLambda.Simple[Unit, INothing] {
  def handle(event: Unit, context: Context[IO], init: Init): IO[Option[INothing]] = IO.none
}
