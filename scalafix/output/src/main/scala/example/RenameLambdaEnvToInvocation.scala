package example

import feral.lambda.Invocation

class Foo[F[_], E] {

  def bar(implicit env: Invocation[F, E]): Unit = ???

}
