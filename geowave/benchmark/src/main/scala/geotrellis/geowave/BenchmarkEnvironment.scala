/*
 * Copyright 2021 Azavea
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

package geotrellis.geowave

import cats.effect.{ContextShift, IO, Timer}
import geotrellis.store.util.BlockingThreadPool

import scala.util.Properties

trait BenchmarkEnvironment {
  val kafka: String     = Properties.envOrElse("KAFKA_HOST", "localhost:9092")
  val cassandra: String = Properties.envOrElse("CASSANDRA_HOST", "localhost")

  implicit val contextShift: ContextShift[IO] = IO.contextShift(BlockingThreadPool.executionContext)
  implicit val timer: Timer[IO]               = IO.timer(BlockingThreadPool.executionContext)
}
