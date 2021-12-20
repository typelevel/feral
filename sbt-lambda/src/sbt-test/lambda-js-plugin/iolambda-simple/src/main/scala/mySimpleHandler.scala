import cats.effect._
import feral.lambda._

object mySimpleHandler extends IOLambda.Simple[Unit, INothing] {
  def apply(event: Unit, context: Context[IO], init: Init): IO[Option[INothing]] = IO.none
}
