package example

// format: off
import cats.effect.IO
import feral.lambda.Invocation
import natchez.Trace
import natchez.EntryPoint
import feral.lambda.natchez.{ AwsTags, KernelSource, TracedHandler }
// format: on

object V0_4_0RewritesInput {

  def useTags = AwsTags.arn("")
  def source = KernelSource.emptyKernelSource[String]
  def tracedHandler[Event, Result](entrypoint: EntryPoint[IO])(
      handler: Trace[IO] => IO[Option[Result]]
  )(
      implicit env: Invocation[IO, Event],
      KS: KernelSource[Event]
  ): IO[Option[Result]] = TracedHandler(entrypoint)(handler)

}
