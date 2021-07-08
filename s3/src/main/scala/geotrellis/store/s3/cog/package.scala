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

package geotrellis.store.s3

import geotrellis.store.s3.util.S3RangeReader
import geotrellis.util.{ByteReader, StreamingByteReader}

import software.amazon.awssdk.services.s3.S3Client

import java.net.URI

package object cog {
  def byteReader(uri: URI, s3Client: S3Client): ByteReader = {
    val auri = new AmazonS3URI(uri)

    StreamingByteReader(
      S3RangeReader(
        bucket = auri.getBucket(),
        key    = auri.getKey(),
        client = s3Client
      )
    )
  }
}
