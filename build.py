import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import zipfile


ROOT = Path(__file__).resolve().parent
ARTIFACT = ROOT / "build" / "bettermyau u6.1.jar"
DEPENDENCIES = (
    "com/google/code/gson/gson/2.2.4/gson-2.2.4.jar",
    "com/google/guava/guava/17.0/guava-17.0.jar",
    "com/mojang/authlib/1.5.21/authlib-1.5.21.jar",
    "io/netty/netty-all/4.0.23.Final/netty-all-4.0.23.Final.jar",
    "net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar",
    "net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-universal.jar",
    "org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar",
    "org/apache/logging/log4j/log4j-api/2.0-beta9-fixed/log4j-api-2.0-beta9-fixed.jar",
    "org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar",
    "org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar",
)


def minecraft_directory(platform=sys.platform, home=None, environ=None):
    home = Path.home() if home is None else Path(home)
    environ = os.environ if environ is None else environ
    if platform == "win32":
        return Path(environ.get("APPDATA", home / "AppData" / "Roaming")) / ".minecraft"
    if platform == "darwin":
        return home / "Library" / "Application Support" / "minecraft"
    return home / ".minecraft"


def arguments(description):
    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--base-jar", type=Path, required=True)
    parser.add_argument("--minecraft-srg", type=Path, required=True)
    parser.add_argument("--libraries", type=Path, default=minecraft_directory() / "libraries")
    parser.add_argument("--jdk", type=Path, default=os.environ.get("JAVA_HOME"))
    return parser.parse_args()


def executable(name, jdk):
    filename = name + (".exe" if os.name == "nt" else "")
    if jdk:
        binary = jdk / filename if jdk.name == "bin" else jdk / "bin" / filename
    else:
        found = shutil.which(filename)
        if not found:
            raise FileNotFoundError("Install JDK 8 or provide --jdk")
        binary = Path(found)
    if not binary.is_file():
        raise FileNotFoundError(binary)
    return str(binary.resolve())


def classpath(args, base=None):
    paths = [base or args.base_jar, args.minecraft_srg]
    paths.extend(args.libraries / dependency for dependency in DEPENDENCIES)
    for path in paths:
        if not path.is_file():
            raise FileNotFoundError(path)
    return [str(path.resolve()) for path in paths]


def compile_java(args, files, classes, paths, directory):
    empty = directory / "sourcepath"
    empty.mkdir(exist_ok=True)
    options = ["-nowarn", "-encoding", "UTF-8", "-source", "8", "-target", "8", "-proc:none",
               "-cp", os.pathsep.join(paths), "-sourcepath", str(empty), "-d", str(classes)]
    options.extend(str(path.resolve()) for path in files)
    argfile = directory / "javac-arguments.txt"
    argfile.write_text("\n".join('"' + option.replace("\\", "/").replace('"', '\\"') + '"'
                                 for option in options), encoding="utf-8")
    subprocess.run([executable("javac", args.jdk), "@" + str(argfile)], check=True)


def source_owner(entry, owners):
    return entry.endswith(".class") and entry[:-6].split("$", 1)[0] in owners


def build(args):
    paths = classpath(args)
    files = sorted((ROOT / "myau").rglob("*.java"))
    owners = {str(path.relative_to(ROOT).with_suffix("")).replace("\\", "/") for path in files}
    ARTIFACT.parent.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="compile-", dir=str(ARTIFACT.parent)) as temporary:
        stage = Path(temporary)
        classes = stage / "classes"
        classes.mkdir()
        compile_java(args, files, classes, paths, stage)
        overlays = {str(path.relative_to(ROOT / "resources")).replace("\\", "/"): path
                    for path in (ROOT / "resources").rglob("*") if path.is_file()}
        overlays.update({str(path.relative_to(classes)).replace("\\", "/"): path
                         for path in classes.rglob("*.class")})
        candidate = stage / ARTIFACT.name
        with zipfile.ZipFile(args.base_jar) as original, zipfile.ZipFile(candidate, "w", zipfile.ZIP_DEFLATED) as output:
            for entry in original.infolist():
                if entry.filename not in overlays and not source_owner(entry.filename, owners):
                    output.writestr(entry, original.read(entry))
            for name, path in sorted(overlays.items()):
                output.write(path, name)
        os.replace(str(candidate), str(ARTIFACT))
    print("Built", ARTIFACT)


if __name__ == "__main__":
    build(arguments("Build BetterMyau 6.1 with JDK 8"))
