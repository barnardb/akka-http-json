/*
 * Copyright 2015 Heiko Seeberger
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

package de.heikoseeberger.akkahttpjson4s

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import org.json4s.{ DefaultFormats, jackson, native }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.{ Duration, DurationInt }

object Json4sSupportSpec {
  case class Foo(bar: String)

  case class MyValueType(value: String) extends AnyVal
}

class Json4sSupportSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  import Json4sSupport._
  import Json4sSupportSpec._

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val formats = DefaultFormats

  val foo = Foo("bar")

  "Json4sSupport" should {
    import system.dispatcher

    "enable marshalling and unmarshalling objects for `DefaultFormats` and `jackson.Serialization`" in {
      implicit val serialization = jackson.Serialization
      val entity = Await.result(Marshal(foo).to[RequestEntity], 100.millis)
      Await.result(Unmarshal(entity).to[Foo], 100.millis) shouldBe foo
    }

    "enable marshalling and unmarshalling objects for default `DefaultFormats` and `native.Serialization`" in {
      implicit val serialization = native.Serialization
      val entity = Await.result(Marshal(foo).to[RequestEntity], 100.millis)
      Await.result(Unmarshal(entity).to[Foo], 100.millis) shouldBe foo
    }

    "not support marshalling future value types because it does not support marshalling value types" in {
      implicit val serialization = native.Serialization

      val value = MyValueType("a value")
      assertTypeError("""Marshal(value).to[RequestEntity]""")

      val futureValue = Future(value)
      // Without Json4sSupportProtection, this would compile
      assertTypeError("""Marshal(futureValue).to[RequestEntity]""")

      /**
       * Without Json4sSupportProtection,
       * Json4sSupport would provide an undesirable marshaller for Future[MyValueType],
       * and this test snippet would compile,
       * and if it were removed from the assertTypeError("""…""") wrapper and the test run,
       * the runtime exception would not get thrown and intercepted.
       *
       * We don't want that confusing behaviour.
       * To avoid it, we make sure there is a type error.
       * Really we want to "assert passes if compiles"; since we can't make it pass, we make it not compile.
       */
      assertTypeError("""
        val e = intercept[RuntimeException] {
          Await.result(Marshal(failedFutureUnit).to[RequestEntity], 100.millis)
        }
        assert(e == exceptionFromFuture)
      """)
    }

  }

  override protected def afterAll() = {
    Await.ready(system.terminate(), Duration.Inf)
    super.afterAll()
  }
}
