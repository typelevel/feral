/* rule=V0_4_0Rewrites */

package example

// format: off
import cats.effect.IO
import feral.lambda.AwsTags
import feral.lambda.KernelSource
import feral.lambda.TracedHandler
import feral.lambda.Invocation
import natchez.Trace
import natchez.EntryPoint
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
