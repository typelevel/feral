/* rule=RenameLambdaEnvToInvocation */

package example

import feral.lambda.LambdaEnv

class Foo[F[_], E] {

  def bar(implicit env: LambdaEnv[F, E]): Unit = ???

}
