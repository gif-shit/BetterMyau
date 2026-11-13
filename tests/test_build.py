from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import build


class BuildChecks(unittest.TestCase):
    def test_launcher_paths(self):
        home = Path("user")
        self.assertEqual(build.minecraft_directory("linux", home, {}), home / ".minecraft")
        self.assertEqual(build.minecraft_directory("darwin", home, {}), home / "Library/Application Support/minecraft")
        self.assertEqual(build.minecraft_directory("win32", home, {"APPDATA": "roaming"}), Path("roaming/.minecraft"))

    def test_replaced_classes(self):
        owners = {"myau/TargetHUDModule", "myau/openb4/ModuleEvents"}
        self.assertTrue(build.source_owner("myau/TargetHUDModule.class", owners))
        self.assertTrue(build.source_owner("myau/TargetHUDModule$OldMode.class", owners))
        self.assertFalse(build.source_owner("myau/TargetHUDModuleExtra.class", owners))
        self.assertFalse(build.source_owner("myau/predac1.json", owners))

    def test_artifact_name(self):
        self.assertEqual(build.ARTIFACT.name, "bettermyau u6.1.jar")


if __name__ == "__main__":
    unittest.main()
