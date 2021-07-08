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

package geotrellis.raster.render

// RGB and RGBA
// Note: GeoTrellis by default expects colors to be in RGBA format.

class RGBA(val int: Int) extends AnyVal {
  def red = (int >> 24) & 0xff
  def green = (int >> 16) & 0xff
  def blue = (int >> 8) & 0xff
  def alpha = int & 0xff
  def isOpaque = (alpha == 255)
  def isTransparent = (alpha == 0)
  def isGrey = (red == green) && (green == blue)
  def unzip = (red, green, blue, alpha)
  def toARGB = (int >> 8) | (alpha << 24)
  def unzipRGBA: (Int, Int, Int, Int) = (red, green, blue, alpha)
  def unzipRGB: (Int, Int, Int) = (red, green, blue)
}

object RGB {
  def apply(r: Int, g: Int, b: Int): Int = ((r << 24) + (g << 16) + (b << 8)) | 0xFF
}

object RGBA {
  def apply(i: Int): RGBA = new RGBA(i)

  def fromRGBA(r: Int, g: Int, b: Int, a: Int): RGBA =
    new RGBA((r << 24) + (g << 16) + (b << 8) + a)

  def fromRGBAPct(r: Int, g: Int, b: Int, alphaPct: Double): RGBA = {
    assert(0 <= alphaPct && alphaPct <= 100)
    fromRGBA(r, g, b, (alphaPct * 2.55).toInt)
  }
}
