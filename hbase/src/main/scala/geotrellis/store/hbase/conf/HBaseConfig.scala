/*
 * Copyright 2018 Azavea
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

package geotrellis.store.hbase.conf

import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class HBaseConfig(catalog: String)

object HBaseConfig {
  lazy val conf: HBaseConfig = ConfigSource.default.at("geotrellis.hbase").loadOrThrow[HBaseConfig]
  implicit def hbaseConfigToClass(obj: HBaseConfig.type): HBaseConfig = conf
}
