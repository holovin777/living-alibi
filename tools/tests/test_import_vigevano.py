import importlib.util
from pathlib import Path
import tempfile
import unittest
import json

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("importer", ROOT/"tools/import_vigevano.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class ImportTest(unittest.TestCase):
    def test_geometry(self):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d)/"map.json"
            result = module.convert(ROOT/"tools/tests/vigevano_fixture.osm", path)
            self.assertEqual(1, result["counts"]["buildings"])
            self.assertGreater(result["counts"]["roads"], 1)
            self.assertEqual(result, json.loads(path.read_text()))
            buildings = [b for c in result["chunks"].values() for b in c["buildings"]]
            self.assertAlmostEqual(9.6, buildings[0]["height"])
            self.assertEqual(4, len(buildings[0]["points"]))
            self.assertAlmostEqual(0, result["spawn"][1])
    def test_project_origin(self):
        self.assertEqual([0.0,0.0], module.project(*module.ORIGIN))

if __name__ == "__main__":
    unittest.main()
