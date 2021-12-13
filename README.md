# feral

feral is a framework for building serverless applications with [Cats Effect](https://github.com/typelevel/cats-effect) targeting both JVM and JavaScript runtimes and deploying them to the cloud.

## Get started

Feral is published for Scala 2.13 and 3.1+ with Scala.js 1.8+.

```sbt
libraryDependencies += "org.typelevel" %%% "feral-lambda" % "0.1.0-M1"
libraryDependencies += "org.typelevel" %%% "feral-lambda-http4s" % "0.1.0-M1"
```


## Motivation

The motivations behind feral

1. **JavaScript is the ideal compile target for serverless applications.** There are a lot of reasons for this, cold-start being one of them, but more generally it's important to remember what the JVM is and is not good at. In particular, the JVM excels at long-lived multithreaded applications which are relatively memory-heavy and reply on medium-lifespan heap allocations. So in other words, persistent microservices.

    Serverless functions are, by definition, not this. They are not persistent, they are (generally) single-threaded, and they need to start very quickly with minimal warming. They do often apply moderate-to-significant heap pressure, but this factor is more than outweighed by the others.

    V8 (the JavaScript engine in Node.js) is a very good runtime for these kinds of use-cases. Realistically, it may be the best-optimized runtime in existence for these requirements, similar to how the JVM is likely the best-optimized runtime in existence for the persistent microservices case.

2. **Scala.js and Cats Effect `IO` are a fantastic for writing code targeting JavaScript.**

   Scala.js does an excellent job preserving Scala's JVM semantics in JavaScript (save a few edge-cases).

   First-class support for JavaScript runtime is a priority of Cats Effect 3. In fact, testing the Cats Effect `IO` on Scala.js revealed a [fairness issue](https://github.com/scala-js/scala-js/issues/4129) that could prevent I/O events and timers from executing reliably. This culminated in [the deprecation](http://www.scala-js.org/news/2021/12/10/announcing-scalajs-1.8.0/#new-compiler-warnings-with-broad-applicability) of the default global `ExecutionContext` in Scala.js v1.8.0. The [`MacrotaskExecutor` project](https://github.com/scala-js/scala-js-macrotask-executor) was extracted from Cats Effect and is now the official recommendation for all Scala.js applications. Cats Effect `IO` is specifically optimized to take advantage of the `MacrotaskExecutor`'s fairness properties while maximizing performance via the use of [auto-yielding](https://typelevel.org/cats-effect/docs/schedulers#yielding).
   
   Furthermore, since [v3.3.0](https://github.com/typelevel/cats-effect/releases/tag/v3.3.0):
    * [Tracing](https://typelevel.org/cats-effect/docs/tracing) 
    * [Fiber dumps](https://github.com/typelevel/cats-effect/releases/tag/v3.3.0) 

3. **Your favorite Typelevel libraries are already on Scala.js.** This means you can directly transfer your knowledge and experience writing Scala for the JVM to writing Scala.js and in many cases _share code_ with your JVM applications.

   The following libraries offer _virtually identical_ APIs across the JVM and JS platforms.
    * [Cats](https://github.com/typelevel/cats) and [Cats Effect](https://github.com/typelevel/cats-effect) for purely functional, asynchronous programming
    * [fs2](https://github.com/typelevel/fs2) and [fs2-io](https://github.com/typelevel/fs2), with support for TCP, UDP, and TLS
    * [http4s](https://github.com/http4s/http4s), including DSLs, server/client middlewares, and ember
    * [natchez](https://github.com/tpolecat/natchez) and [natchez-http4s](https://github.com/tpolecat/natchez-http4s) for tracing, including an AWS X-Ray integration
    * [skunk](https://github.com/tpolecat/skunk) for Postgres/Redshift and [rediculous](https://github.com/davenverse/rediculous) for Redis
    * [circe](https://github.com/circe/circe), [scodec](https://github.com/scodec/scodec) and [scodec-bits](https://github.com/scodec/scodec-bits) for encoders/decoders
