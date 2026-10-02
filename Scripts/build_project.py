import argparse
import os
from pathlib import Path
import shutil
import subprocess


def build_module(module_name, game_dir, dependency_jars, compiler, jar):
    project_root = Path(__file__).resolve().parents[1]
    module_root = project_root / "Scripts" / module_name
    output_root = project_root / "Builds" / module_name
    classes = output_root / "classes"
    classes.mkdir(parents=True, exist_ok=True)
    sources = sorted((module_root / "source").rglob("*.java"))
    classpath = os.pathsep.join(str(path) for path in [*sorted(game_dir.glob("*.jar")), *dependency_jars])
    subprocess.run([*compiler, "-encoding", "UTF-8", "-classpath", classpath, "-d", str(classes), *map(str, sources)], check=True)
    mod_root = output_root / "mod"
    shutil.copytree(module_root / "mod", mod_root, dirs_exist_ok=True)
    package = "local/vpads" if module_name == "NativeAim" else "local/vpinteriors"
    filename = "ViewpointNativeADS.jar" if module_name == "NativeAim" else "ViewpointInteriors.jar"
    destination = mod_root / "42" / "media" / "java" / "client" / filename
    destination.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([jar, "--create", "--file", str(destination), "-C", str(classes), package], check=True)
    print(f"Built {module_name}: {destination.relative_to(project_root)}")
    print("No game files were installed or changed. Model assets are integrated separately.")


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
