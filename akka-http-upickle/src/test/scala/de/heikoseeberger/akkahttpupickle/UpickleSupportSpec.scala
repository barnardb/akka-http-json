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

package de.heikoseeberger.akkahttpupickle

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpupickle.UpickleSupportSpec.MyValueType
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.{ Duration, DurationInt }

object UpickleSupportSpec {
  case class MyValueType(value: String) extends AnyVal
}

class UpickleSupportSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  import UpickleSupport._

  case class Foo(bar: String)

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  "UpickleSupport" should {
    import system.dispatcher

    "enable marshalling and unmarshalling of case classes" in {
      val foo = Foo("bar")
      val entity = Await.result(Marshal(foo).to[RequestEntity], 100.millis)
      Await.result(Unmarshal(entity).to[Foo], 100.millis) shouldBe foo
    }

    "support marshalling future value types" in {
      val value = MyValueType("a value")
      val entityFromPlain = Await.result(Marshal(value).to[RequestEntity], 100.millis)
      val entityFromFuture = Await.result(Marshal(Future(value)).to[RequestEntity], 100.millis)
      entityFromFuture shouldBe entityFromPlain
    }
  }

  override protected def afterAll() = {
    Await.ready(system.terminate(), Duration.Inf)
    super.afterAll()
  }
}
