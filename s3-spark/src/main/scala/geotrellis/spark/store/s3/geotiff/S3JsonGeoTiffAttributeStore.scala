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

package geotrellis.spark.store.s3.geotiff

import geotrellis.store.s3.{AmazonS3URI, S3ClientProducer}
import geotrellis.spark.store.hadoop.geotiff._
import geotrellis.util.annotations.experimental

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, PutObjectRequest}
import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import cats.syntax.either._

import java.net.URI
import scala.io.Source

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object S3JsonGeoTiffAttributeStore {
  @experimental def readData(uri: URI, s3Client: => S3Client = S3ClientProducer.get()): List[GeoTiffMetadata] = {

    val s3Uri = new AmazonS3URI(uri)
    val request = GetObjectRequest.builder()
      .bucket(s3Uri.getBucket)
      .key(s3Uri.getKey)
      .build()

    val objStream = s3Client.getObject(request)

    val json = try {
      Source
        .fromInputStream(objStream)
        .getLines()
        .mkString(" ")
    } finally objStream.close()

    parse(json).flatMap(_.as[List[GeoTiffMetadata]]).valueOr(throw _)
  }

  @experimental def readDataAsTree(uri: URI, s3Client: => S3Client = S3ClientProducer.get()): GeoTiffMetadataTree[GeoTiffMetadata] =
    GeoTiffMetadataTree.fromGeoTiffMetadataSeq(readData(uri, s3Client))

  def apply(
    uri: URI,
    s3Client: => S3Client
  ): JsonGeoTiffAttributeStore = {
    // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
    JsonGeoTiffAttributeStore(uri, readDataAsTree(_, s3Client))
  }

  def apply(
    path: URI,
    name: String,
    uri: URI,
    pattern: String,
    recursive: Boolean = true,
    s3Client:  S3Client = S3ClientProducer.get()
  ): JsonGeoTiffAttributeStore = {
    val s3Uri = new AmazonS3URI(path)
    val data = S3GeoTiffInput.list(name, uri, pattern, recursive)
    // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
    val attributeStore = JsonGeoTiffAttributeStore(path, readDataAsTree(_, s3Client))

    val str = data.asJson.noSpaces
    val request = PutObjectRequest.builder()
      .bucket(s3Uri.getBucket())
      .key(s3Uri.getKey())
      .build()
    s3Client.putObject(request, RequestBody.fromBytes(str.getBytes("UTF-8")))

    attributeStore
  }
}
