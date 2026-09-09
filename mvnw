#!/usr/bin/env bash
# mvnw - 后端学习仓库（E:\houduan）通用的 Maven 包装脚本
# 解决两个环境坑：
#   1. Git Bash MSYS 会把 /f/... 转成 \f\...，导致 java 找不到 classpath
#   2. 全局 huaweicloud 镜像偶尔超时，本项目统一用 aliyun 镜像
#
# 用法（在任一 Maven 项目目录下执行）：
#   cd multi-module-project && ../mvnw clean package
#   ../mvnw -pl 01-quick-start -am package
set -e

# Windows 风格的 MAVEN_HOME（避免 MSYS 路径转换）
export JAVA_HOME='E:\jdk'
export PATH="$JAVA_HOME\\bin:$PATH"
export MAVEN_HOME='F:\heima\apache-maven-3.9.16'

# Maven 项目目录 = 调用者的当前工作目录（Windows 路径）
PROJECT_DIR="$(pwd -W 2>/dev/null || pwd)"

# settings.xml 固定用仓库根目录下的 .tools（aliyun 镜像）
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -W 2>/dev/null || pwd)"
SETTINGS="${SCRIPT_DIR}\\.tools\\settings-aliyun.xml"

# 直接调用 java 启动 plexus-classworlds，绕开 mvn bash 脚本的路径问题
exec /e/jdk/bin/java \
  -classpath "${MAVEN_HOME}\\boot\\plexus-classworlds-2.11.0.jar" \
  "-Dclassworlds.conf=${MAVEN_HOME}\\bin\\m2.conf" \
  "-Dmaven.home=${MAVEN_HOME}" \
  "-Dmaven.multiModuleProjectDirectory=${PROJECT_DIR}" \
  org.codehaus.plexus.classworlds.launcher.Launcher \
  -s "${SETTINGS}" \
  "$@"
