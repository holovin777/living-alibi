import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import import_vigevano as osm


class OverpassDownloadTests(unittest.TestCase):
    def test_wide_tiles(self):
        tiles = osm.tiles(osm.WIDE_BBOX)
        self.assertEqual(len(tiles), 4)
        self.assertEqual(osm.tiles(osm.BBOX), [osm.BBOX])
        self.assertEqual(tiles[0][:2], osm.WIDE_BBOX[:2])
        self.assertEqual(tiles[-1][2:], osm.WIDE_BBOX[2:])

    def test_merge_prefers_tagged_node_and_deduplicates(self):
        first = ET.fromstring('<osm><node id="1" lat="45" lon="8"/><way id="2"><nd ref="1"/></way></osm>')
        second = ET.fromstring('<osm><node id="1" lat="45" lon="8"><tag k="natural" v="tree"/></node><way id="2"><nd ref="1"/></way></osm>')
        merged = osm.merge_tiles([first, second])
        self.assertEqual(len(merged.findall("node")), 1)
        self.assertIsNotNone(merged.find("node/tag"))
        self.assertEqual(len(merged.findall("way")), 1)

    def test_failed_tile_preserves_cache(self):
        with tempfile.TemporaryDirectory() as work:
            path = Path(work) / "map.osm"
            path.write_text("old")
            with patch.object(osm, "download_tile", side_effect=RuntimeError("offline")):
                with self.assertRaises(RuntimeError):
                    osm.download(path, osm.WIDE_BBOX)
            self.assertEqual(path.read_text(), "old")

    def test_retry_other_endpoint(self):
        import io
        import urllib.error
        xml = b'<osm><node id="1"/><way id="2"/></osm>'
        with tempfile.TemporaryDirectory() as work:
            with patch.object(osm.urllib.request, "urlopen", side_effect=[urllib.error.HTTPError("https://example.com", 504, "timeout", {}, None), io.BytesIO(xml)]) as call:
                root = osm.download_tile(osm.BBOX, Path(work))
                self.assertEqual(root.tag, "osm")
                self.assertEqual(call.call_count, 2)
                self.assertEqual(call.call_args.args[0].full_url, osm.OVERPASS_ENDPOINTS[1])
