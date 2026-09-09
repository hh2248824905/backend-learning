#!/usr/bin/env bash
# mvnw - 本项目专用的 Maven 包装脚本
# 解决两个环境坑：
#   1. Git Bash MSYS 会把 /f/... 转成 \f\...，导致 java 找不到 classpath
#   2. 全局 huaweicloud 镜像偶尔超时，本项目使用 aliyun 镜像
#
# 用法：./mvnw clean test
#      ./mvnw package
#      ./mvnw -pl hello-app -am test
set -e

# Windows 风格的 MAVEN_HOME（避免 MSYS 路径转换）
export JAVA_HOME='E:\jdk'
export PATH="$JAVA_HOME\\bin:$PATH"
export MAVEN_HOME='F:\heima\apache-maven-3.9.16'

# 项目级 settings.xml（用 aliyun 镜像，避免 huaweicloud 握手超时）
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd -W 2>/dev/null || pwd)"
SETTINGS="${PROJECT_DIR}\\.tools\\settings-aliyun.xml"

# 直接调用 java 启动 plexus-classworlds，绕开 mvn bash 脚本的路径问题
exec /e/jdk/bin/java \
  -classpath "${MAVEN_HOME}\\boot\\plexus-classworlds-2.11.0.jar" \
  "-Dclassworlds.conf=${MAVEN_HOME}\\bin\\m2.conf" \
  "-Dmaven.home=${MAVEN_HOME}" \
  "-Dmaven.multiModuleProjectDirectory=${PROJECT_DIR}" \
  org.codehaus.plexus.classworlds.launcher.Launcher \
  -s "${SETTINGS}" \
  "$@"
