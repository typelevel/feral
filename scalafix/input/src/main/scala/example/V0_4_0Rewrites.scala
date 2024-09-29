/* rule=V0_4_0Rewrites */

package example

// format: off
import feral.lambda.AwsTags
import feral.lambda.KernelSource
import feral.lambda.TracedHandler
// format: on

object V0_4_0RewritesInput {

  val tags: AwsTags = ???
  val source: KernelSource = ???
  val handler: TracedHandler = ???

}
