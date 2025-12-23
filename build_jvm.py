#!/usr/bin/env python3
"""
构建 yogurt-jvm-all.jar 的脚本

使用方法:
    python build_jvm.py [--proxy HOST:PORT] [--java-home PATH]

示例:
    python build_jvm.py
    python build_jvm.py --proxy 192.168.31.84:7890
    python build_jvm.py --proxy 127.0.0.1:7890 --java-home "D:\\SSoftwareFiles\\JDKs\\zulu21"
"""

import os
import sys
import subprocess
import argparse
from pathlib import Path


def find_java_home():
    """查找可用的 Java 21+ 路径"""
    # 1. 检查环境变量
    java_home = os.environ.get("JAVA_HOME")
    if java_home and Path(java_home).exists():
        return java_home
    
    # 2. 常见的 JDK 安装位置
    common_paths = [
        # Windows
        r"D:\SSoftwareFiles\JDKs\zulu21.42.19-ca-jdk21.0.7-win_x64",
        r"C:\Program Files\Java\jdk-21",
        r"C:\Program Files\Eclipse Adoptium\jdk-21",
        r"C:\Program Files\Zulu\zulu-21",
        # Linux/macOS
        "/usr/lib/jvm/java-21-openjdk",
        "/usr/lib/jvm/java-21",
        "/opt/java/jdk-21",
    ]
    
    for path in common_paths:
        if Path(path).exists():
            return path
    
    return None


def run_gradle(project_root: Path, proxy_host: str = None, proxy_port: str = None, java_home: str = None):
    """运行 Gradle 构建"""
    
    # 设置环境变量
    env = os.environ.copy()
    
    if java_home:
        env["JAVA_HOME"] = java_home
        print(f"✓ JAVA_HOME: {java_home}")
    
    # 构建 Gradle 命令
    if sys.platform == "win32":
        gradle_cmd = str(project_root / "gradlew.bat")
    else:
        gradle_cmd = str(project_root / "gradlew")
        # 确保有执行权限
        os.chmod(gradle_cmd, 0o755)
    
    cmd = [gradle_cmd, ":yogurt-jvm:buildFatJar", "--no-daemon"]
    
    # 添加代理设置
    if proxy_host and proxy_port:
        cmd.extend([
            f"-Dhttp.proxyHost={proxy_host}",
            f"-Dhttp.proxyPort={proxy_port}",
            f"-Dhttps.proxyHost={proxy_host}",
            f"-Dhttps.proxyPort={proxy_port}",
        ])
        print(f"✓ 代理: {proxy_host}:{proxy_port}")
    
    print(f"✓ 工作目录: {project_root}")
    print(f"✓ 执行命令: {' '.join(cmd)}")
    print("-" * 60)
    
    # 执行构建
    try:
        process = subprocess.run(
            cmd,
            cwd=project_root,
            env=env,
            # 不捕获输出，直接显示到终端
        )
        return process.returncode
    except KeyboardInterrupt:
        print("\n\n⚠ 构建被用户中断")
        return 1
    except Exception as e:
        print(f"\n✗ 构建出错: {e}")
        return 1


def main():
    parser = argparse.ArgumentParser(
        description="构建 yogurt-jvm-all.jar",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
    python build_jvm.py
    python build_jvm.py --proxy 192.168.31.84:7890
    python build_jvm.py --proxy 127.0.0.1:7890 --java-home "D:\\JDKs\\jdk-21"
        """
    )
    parser.add_argument(
        "--proxy", 
        type=str, 
        help="代理服务器地址 (格式: HOST:PORT，例如 192.168.31.84:7890)"
    )
    parser.add_argument(
        "--java-home",
        type=str,
        help="Java 21+ 安装路径"
    )
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("🍦 Yogurt JVM 构建脚本")
    print("=" * 60)
    
    # 获取项目根目录
    script_dir = Path(__file__).parent.resolve()
    project_root = script_dir
    
    # 检查是否在正确的目录
    if not (project_root / "gradlew.bat").exists() and not (project_root / "gradlew").exists():
        print("✗ 错误: 找不到 gradlew，请确保脚本在项目根目录")
        return 1
    
    # 查找 Java
    java_home = args.java_home or find_java_home()
    if not java_home:
        print("✗ 错误: 找不到 Java 21+，请使用 --java-home 参数指定")
        print("  示例: python build_jvm.py --java-home \"D:\\JDKs\\jdk-21\"")
        return 1
    
    # 解析代理
    proxy_host = None
    proxy_port = None
    if args.proxy:
        try:
            proxy_host, proxy_port = args.proxy.split(":")
        except ValueError:
            print(f"✗ 错误: 代理格式不正确，应为 HOST:PORT")
            return 1
    
    print()
    
    # 运行构建
    ret = run_gradle(project_root, proxy_host, proxy_port, java_home)
    
    print()
    print("=" * 60)
    
    if ret == 0:
        jar_path = project_root / "yogurt-jvm" / "build" / "libs" / "yogurt-jvm-all.jar"
        if jar_path.exists():
            size_mb = jar_path.stat().st_size / (1024 * 1024)
            print(f"✓ 构建成功!")
            print(f"✓ JAR 文件: {jar_path}")
            print(f"✓ 文件大小: {size_mb:.2f} MB")
        else:
            print("✓ 构建完成，但找不到 JAR 文件")
            print(f"  预期位置: {jar_path}")
    else:
        print(f"✗ 构建失败 (退出码: {ret})")
        print()
        print("常见问题排查:")
        print("  1. 网络问题 - 尝试使用 --proxy 参数")
        print("  2. Java 版本 - 确保使用 Java 21+")
        print("  3. 查看上方的错误信息")
    
    print("=" * 60)
    return ret


if __name__ == "__main__":
    sys.exit(main())
