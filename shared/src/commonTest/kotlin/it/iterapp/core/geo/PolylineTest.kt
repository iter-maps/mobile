package it.iterapp.core.geo

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolylineTest {

  @Test
  fun decodesGoogleReferencePolyline() {
    // The worked example from the encoded-polyline format documentation.
    val points = decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
    assertEquals(3, points.size)
    assertTrue(abs(points[0].lat - 38.5) < 1e-9)
    assertTrue(abs(points[0].lon - (-120.2)) < 1e-9)
    assertTrue(abs(points[1].lat - 40.7) < 1e-9)
    assertTrue(abs(points[2].lon - (-126.453)) < 1e-9)
  }

  @Test
  fun emptyAndMalformedInputAreSafe() {
    assertEquals(0, decodePolyline("").size)
    // Truncated tail: returns what it could decode, never throws.
    decodePolyline("_p~iF~ps|U_ul")
  }
}

class BBoxFormatTest {

  @Test
  fun formatsInWireOrder() {
    assertEquals("11.3,41.1,14.05,43.35", BBoxFormat.format(11.3, 41.1, 14.05, 43.35))
  }

  @Test
  fun validationMirrorsServerOrder() {
    assertEquals(null, BBoxFormat.validate(11.3, 41.1, 14.05, 43.35))
    assertEquals("BBOX_OUT_OF_RANGE", BBoxFormat.validate(-190.0, 41.1, 14.05, 43.35))
    assertEquals("BBOX_DEGENERATE", BBoxFormat.validate(14.05, 41.1, 11.3, 43.35))
    assertEquals("BBOX_DEGENERATE", BBoxFormat.validate(11.3, 41.1, 11.3, 43.35))
  }

  @Test
  fun gtfsLocalIdStripsFeedPrefix() {
    assertEquals("70001", gtfsLocalId("ATAC:70001"))
    assertEquals("70001", gtfsLocalId("70001"))
  }
}
