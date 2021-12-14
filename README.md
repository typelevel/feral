# feral [![Discord](https://img.shields.io/discord/632277896739946517.svg?label=&logo=discord&logoColor=ffffff&color=404244&labelColor=6A7EC2)](https://discord.gg/AJASeCq8gN)

feral is a framework for writing serverless functions in Scala with [Cats Effect](https://github.com/typelevel/cats-effect) and deploying them to the cloud, targeting both JVM and JavaScript runtimes. Integrations with libraries such as [circe](https://github.com/circe/circe) and [http4s](https://github.com/http4s/http4s) enable feral to provide an idiomatic, purely functional interface that is flexible and composable. The initial focus has been on supporting [AWS Lambda](https://aws.amazon.com/lambda/) and will eventually expand to other serverless providers.

## Getting started

Feral is published for Scala 2.13 and 3.1+ with artifacts for both JVM and Scala.js 1.8+.

```scala
// Everything you need to write your AWS Lambda
libraryDependencies += "org.typelevel" %%% "feral-lambda" % "0.1.0-M1"

// Specialized integrations
libraryDependencies += "org.typelevel" %%% "feral-lambda-http4s" % "0.1.0-M1"
libraryDependencies += "org.typelevel" %%% "feral-lambda-cloudformation-custom-resource" % "0.1.0-M1"
```

`IOLambda[Event, Result]` or `IOLambda.Simple[Event, Result]` are the entrypoint for your Lambda (comparable to `IOApp` in Cats Effect). `Event` and `Result` can be any type for which a circe `Decoder` and `Encoder` are defined and must correspond to the "trigger" event and (optional) result for your Lambda. The `feral.lambda.events` package provides some of these models already ([please consider contributing!](https://github.com/typelevel/feral/issues/48)).

The [examples](https://github.com/typelevel/feral/tree/main/examples/src/main/scala/feral/examples) provide an in-depth demonstration of how to implement your Lambda.

## Why go feral?

The premise that you can (and should!) write production-ready serverless functions in Scala targeting JavaScript may be a surprising one. Here, we motivate feral—and the rapid maturity of the Typelevel.js ecosystem—based on three ideas.

1. **JavaScript is the ideal compile target for serverless functions.** 
  
    There are a lot of reasons for this, cold-start being one of them, but more generally it's important to remember what the JVM is and is not good at. In particular, the JVM excels at long-lived multithreaded applications which are relatively memory-heavy and reply on medium-lifespan heap allocations. So in other words, persistent microservices.

    Serverless functions are, by definition, not this. They are not persistent, they are (generally) single-threaded, and they need to start very quickly with minimal warming. They do often apply moderate-to-significant heap pressure, but this factor is more than outweighed by the others.

    V8 (the JavaScript engine in Node.js) is a very good runtime for these kinds of use-cases. Realistically, it may be the best-optimized runtime in existence for these requirements, similar to how the JVM is likely the best-optimized runtime in existence for the persistent microservices case.

2. **Scala.js and Cats Effect work together to provide powerful, well-defined semantics for writing JavaScript applications.**

   It hopefully should not take much convincing that Scala is a fantastic language to use, regardless of the ultimate compile target. But what might be unexpected by those new to Scala.js is how well it preserves Scala's JVM semantics in JavaScript. Save a few [edge-cases](https://www.scala-js.org/doc/semantics.html), by and large Scala programs behave the same on JS as they do on the JVM.

   Cats Effect takes this a step further by establishing [semantics for _asynchronous_ programming](https://typelevel.org/cats-effect/docs/typeclasses) (aka "laws") and guaranteeing them across the JVM and JS. In fact, the initial testing of these semantics on Scala.js revealed a [fundamental fairness issue](https://github.com/scala-js/scala-js/issues/4129) that culminated in [the deprecation](http://www.scala-js.org/news/2021/12/10/announcing-scalajs-1.8.0/#new-compiler-warnings-with-broad-applicability) of the default global `ExecutionContext` in Scala.js. As a replacement, the [`MacrotaskExecutor` project](https://github.com/scala-js/scala-js-macrotask-executor) was extracted from Cats Effect and is now the official recommendation for all Scala.js applications. Cats Effect `IO` is specifically optimized to take advantage of the `MacrotaskExecutor`'s fairness properties while maximizing throughput and performance.

   Besides the focus on performance, `IO` also has features to enrich the observability and debuggability of your JavaScript applications during development.
    * [Tracing and enhanced exceptions](https://typelevel.org/cats-effect/docs/tracing) that enable you to follow the execution graph of a process in your program, even across asynchronous boundaries.
    * [Fiber dumps](https://github.com/typelevel/cats-effect/releases/tag/v3.3.0) to introspect the traces of _all_ the concurrent processes in your program at a given time.

3. **Your favorite Typelevel libraries are already designed for Scala.js.**

   Thanks to the platform-independent semantics, software built using abstractions from Cats Effect and other Typelevel libraries can often be easily cross-compiled for Scala.js. One spectacular example of this is [skunk](https://github.com/tpolecat/skunk), a data access library for Postgres that was never intended to compile to JavaScript. However, due to its whole-hearted adoption of purely functional asynchronous programming, today it also runs on Node.js with _virtually no changes to its source code_.

   In practice, this means you can directly transfer your knowledge and experience writing Scala for the JVM to writing Scala.js and in many cases _share code_ with your JVM applications. The following libraries offer _identical_ APIs across the JVM and JS platforms:
    * [Cats](https://github.com/typelevel/cats) and [Cats Effect](https://github.com/typelevel/cats-effect) for purely functional, asynchronous programming
    * [fs2](https://github.com/typelevel/fs2) and [fs2-io](https://github.com/typelevel/fs2), with support for TCP, UDP, and TLS
    * [http4s](https://github.com/http4s/http4s), including DSLs, server/client middlewares, and ember
    * [natchez](https://github.com/tpolecat/natchez) and [natchez-http4s](https://github.com/tpolecat/natchez-http4s) for tracing
    * [skunk](https://github.com/tpolecat/skunk) for Postgres/Redshift and [rediculous](https://github.com/davenverse/rediculous) for Redis
    * [circe](https://github.com/circe/circe), [scodec](https://github.com/scodec/scodec) and [scodec-bits](https://github.com/scodec/scodec-bits) for encoders/decoders
    * and more ...
