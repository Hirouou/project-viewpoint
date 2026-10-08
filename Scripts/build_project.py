import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


def build_module(module_name, game_dir, dependency_jars, compiler, jar):
    project_root = Path(__file__).resolve().parents[1]
    module_root = project_root / "Scripts" / module_name
    output_root = project_root / "Builds" / module_name
    output_root.mkdir(parents=True, exist_ok=True)
    # Clean compilation is required when a release removes hooks/resources.
    temporary = tempfile.TemporaryDirectory(prefix="compile-", dir=output_root)
    classes = Path(temporary.name) / "classes"
    classes.mkdir()
    sources = sorted((module_root / "source").rglob("*.java"))
    classpath = os.pathsep.join(str(path) for path in [*sorted(game_dir.glob("*.jar")), *dependency_jars])
    subprocess.run([*compiler, "-encoding", "UTF-8", "-classpath", classpath, "-d", str(classes), *map(str, sources)], check=True)
    # Include only resources still present in the current source tree.
    for resource in (module_root / "source").rglob("*"):
        if resource.is_file() and resource.suffix not in {".java", ".class"}:
            copied = classes / resource.relative_to(module_root / "source")
            copied.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(resource, copied)
    mod_root = Path(temporary.name) / "mod"
    shutil.copytree(module_root / "mod", mod_root)
    package = "local/vpads" if module_name == "NativeAim" else "local/vpinteriors"
    filename = "ViewpointNativeADS.jar" if module_name == "NativeAim" else "ViewpointInteriors.jar"
    destination = mod_root / "42" / "media" / "java" / "client" / filename
    destination.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([jar, "--create", "--file", str(destination), "-C", str(classes), package], check=True)
    final = output_root / "mod"
    # Keep the prior output as a recoverable snapshot instead of merging files
    # which could revive models or patches removed from the current source.
    if final.exists():
        previous = output_root / ("previous-" + Path(temporary.name).name)
        final.rename(previous)
    mod_root.rename(final)
    temporary.cleanup()
    print(f"Built {module_name}: {(final / destination.relative_to(mod_root)).relative_to(project_root)}")
    print("No game files were installed or changed.")


def main():
    parser = argparse.ArgumentParser(description="Build original Project ViewPoint code with locally installed dependencies.")
    parser.add_argument("--module", choices=["NativeAim", "Interiors", "all"], default="all")
    parser.add_argument("--game-dir", required=True, type=Path)
    parser.add_argument("--dependency-jar", required=True, action="append", type=Path)
    parser.add_argument("--jdk-dir", type=Path)
    parser.add_argument("--compiler-jar", type=Path)
    parser.add_argument("--java-bin", type=Path)
    args = parser.parse_args()
    if not args.game_dir.is_dir() or not list(args.game_dir.glob("*.jar")):
        parser.error("--game-dir must point to a local Project Zomboid installation containing its JAR files.")
    dependency_jars = [path.resolve() for path in args.dependency_jar]
    if any(not path.is_file() for path in dependency_jars):
        parser.error("Every --dependency-jar must point to an existing dependency.")
    suffix = ".exe" if os.name == "nt" else ""
    javac = str(args.jdk_dir / "bin" / ("javac" + suffix)) if args.jdk_dir else shutil.which("javac")
    jar = str(args.jdk_dir / "bin" / ("jar" + suffix)) if args.jdk_dir else shutil.which("jar")
    if not jar or (not javac and not args.compiler_jar):
        parser.error("Provide JDK tools with --jdk-dir or PATH, or an ECJ compiler with a JDK jar tool.")
    if args.compiler_jar:
        if not args.compiler_jar.is_file():
            parser.error("--compiler-jar must point to an existing Eclipse ECJ JAR.")
        java = args.java_bin or args.game_dir / "jre64" / "bin" / ("java" + suffix)
        if not java.is_file():
            parser.error("Provide a Java runtime compatible with the game and compiler via --java-bin.")
        compiler = [str(java), "-jar", str(args.compiler_jar.resolve()), "-17", "-warn:none"]
    else:
        compiler = [javac, "--release", "17"]
    modules = ["NativeAim", "Interiors"] if args.module == "all" else [args.module]
    for module_name in modules:
        build_module(module_name, args.game_dir.resolve(), dependency_jars, compiler, jar)


if __name__ == "__main__":
    main()
