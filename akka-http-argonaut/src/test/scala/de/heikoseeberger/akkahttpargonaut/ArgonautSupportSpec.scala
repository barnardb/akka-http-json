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

package de.heikoseeberger.akkahttpargonaut

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.{ Duration, DurationInt }

object ArgonautSupportSpec {
  case class Foo(bar: String)
}

class ArgonautSupportSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  import ArgonautSupport._
  import ArgonautSupportSpec._

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val foo = Foo("bar")

  "ArgonautSupport" should {
    import system.dispatcher

    "enable marshalling and unmarshalling objects for generic derivation" in {
      import argonaut.Argonaut._
      implicit def FooCodec = casecodec1(Foo.apply, Foo.unapply)("bar")

      val entity = Await.result(Marshal(foo).to[RequestEntity], 100.millis)
      Await.result(Unmarshal(entity).to[Foo], 100.millis) shouldBe foo
    }

    "not interfere with Future unwrapping for Unit" in {
      val exceptionFromFuture = new RuntimeException
      val failedFutureUnit = Future.failed[Unit](exceptionFromFuture)

      val e = intercept[RuntimeException] {
        Await.result(Marshal(failedFutureUnit).to[RequestEntity], 100.millis)
      }
      assert(e == exceptionFromFuture)
    }
  }

  override protected def afterAll() = {
    Await.ready(system.terminate(), Duration.Inf)
    super.afterAll()
  }
}
