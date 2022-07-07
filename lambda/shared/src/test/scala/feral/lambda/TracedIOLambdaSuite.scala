/*
 * Copyright 2021 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feral.lambda

import cats.effect.{IO, Resource}
import cats.effect.kernel.Ref
import cats.implicits._
import munit.CatsEffectSuite
import natchez._

import java.net.URI

class TracedIOLambdaSuite extends CatsEffectSuite {
  case class MockCompleteSpan(
      name: String,
      fields: Map[String, TraceValue] = Map.empty,
      children: Vector[MockCompleteSpan] = Vector.empty)

  case class MockSpan(
      name: String,
      fields: Ref[IO, Map[String, TraceValue]],
      children: Ref[IO, Vector[MockCompleteSpan]])
      extends Span[IO] {
    override def put(addFields: (String, TraceValue)*): IO[Unit] = fields.update(_ ++ addFields)

    override def kernel: IO[Kernel] = IO.pure(Kernel(Map.empty))

    override def span(name: String): Resource[IO, Span[IO]] =
      Resource.make(MockSpan.create(name))(MockSpan.finalize(_, children))

    override def traceId: IO[Option[String]] = IO.none

    override def spanId: IO[Option[String]] = IO.none

    override def traceUri: IO[Option[URI]] = IO.none

  }

  object MockSpan {
    def create(name: String): IO[MockSpan] =
      (IO.ref(Map.empty[String, TraceValue]), IO.ref(Vector.empty[MockCompleteSpan])).mapN {
        case (fields, children) => new MockSpan(name, fields, children)
      }

    def finalize(span: MockSpan, appendTo: Ref[IO, Vector[MockCompleteSpan]]): IO[Unit] = for {
      fields <- span.fields.get
      children <- span.children.get
      completeSpan = MockCompleteSpan(span.name, fields, children)
      _ <- appendTo.update(_.appended(completeSpan))
    } yield ()
  }

  case class MockEntryPoint(children: Ref[IO, Vector[MockCompleteSpan]])
      extends EntryPoint[IO] {

    override def root(name: String): Resource[IO, Span[IO]] =
      continueOrElseRoot(name, Kernel(Map.empty))

    override def continue(name: String, kernel: Kernel): Resource[IO, Span[IO]] =
      continueOrElseRoot(name, kernel)

    override def continueOrElseRoot(name: String, kernel: Kernel): Resource[IO, Span[IO]] =
      Resource.make(MockSpan.create(name))(MockSpan.finalize(_, children))
  }

  trait MockResource {
    def query(s: String): IO[Option[String]]
  }
  object MockResource {
    def apply(trace: Trace[IO]): Resource[IO, MockResource] =
      Resource.eval(trace.span("init")(IO(new MockResource {
        override def query(s: String): IO[Option[String]] = trace.span("query")(IO.some(s))
      })))
  }

  test("Trace produces correct spans") {
    implicit val ks: KernelSource[String] = KernelSource.emptyKernelSource
    for {
      spansRecorder <- IO.ref(Vector.empty[MockCompleteSpan])
      entryPoint = MockEntryPoint(spansRecorder)
      tracedLambda <- IO(new TracedIOLambda[String, String](IO.pure(entryPoint)) {
        def handler: (LambdaEnv[IO, String], Trace[IO]) => Resource[IO, IO[Option[String]]] = {
          (env, trace) => MockResource(trace).map(r => env.event.flatMap(r.query))
        }
      })
      _ <- ('0' to '2')
        .map(_.toString)
        .toList
        .traverse(e => tracedLambda.setupAndRun(e, mockContext(e)).assertEquals(Some(e)))
      _ <- spansRecorder
        .get
        .assertEquals(
          Vector(
            expectRootSpan("0", Vector(MockCompleteSpan("init"), MockCompleteSpan("query"))),
            expectRootSpan("1", Vector(MockCompleteSpan("query"))),
            expectRootSpan("2", Vector(MockCompleteSpan("query")))
          ))
    } yield ()
  }

  def mockContext(reqId: String) =
    new Context[IO]("funcName", "", "funcArn", 0, reqId, "", "", None, None, IO.never)

  def expectRootSpan(
      requestId: String,
      children: Vector[MockCompleteSpan] = Vector.empty): MockCompleteSpan =
    MockCompleteSpan(
      "funcName",
      Map("aws.arn" -> "funcArn", "aws.requestId" -> requestId),
      children)
}
