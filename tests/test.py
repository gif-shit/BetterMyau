import os
from pathlib import Path
import subprocess
import sys
import tempfile

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import build


def main():
    args = build.arguments("Run the BetterMyau headless regression suite")
    paths = build.classpath(args, build.ARTIFACT)
    paths += [str(args.libraries / "commons-io/commons-io/2.4/commons-io-2.4.jar"),
              str(args.libraries / "org/apache/logging/log4j/log4j-core/2.0-beta9/log4j-core-2.0-beta9.jar")]
    for path in paths:
        if not Path(path).is_file():
            raise FileNotFoundError(path)
    with tempfile.TemporaryDirectory(prefix="codebase-tests-", dir=str(build.ROOT / "build")) as temporary:
        stage = Path(temporary)
        classes = stage / "classes"
        classes.mkdir()
        build.compile_java(args, sorted((build.ROOT / "tests").rglob("*.java")), classes, paths, stage)
        command = [build.executable("java", args.jdk), "-ea", "-cp", os.pathsep.join([str(classes)] + paths)]
        tests = ["myau.FlyRegression", "myau.FlyHookCheck", "myau.ScaffoldIntegrationRegression", "PitSwapChecks",
                 "myau.PitSwapIntegrationRegression", "myau.TrajectoriesRegression", "myau.CodebaseRegression", "myau.Update60Regression", "myau.ClickGuiCategoriesRegression", "myau.ItemSpoofBlockRegression", "myau.ItemEffectsRegression", "myau.ESPHealthBarRegression"]
        for test in tests:
            subprocess.run(command + [test, str(args.minecraft_srg.resolve()), str(build.ARTIFACT)], cwd=str(stage), check=True)
    subprocess.run([sys.executable, "-m", "unittest", "discover", "-s", str(build.ROOT / "tests"), "-p", "test_build.py"], check=True)


if __name__ == "__main__":
    main()
