#!/usr/bin/env bash
# 本沙箱(Git Bash 缺 cygpath)里跑 Maven 的兜底脚本。
# 直接用 Java 拉起 classworlds 启动器，所有路径用 Windows 风格(D:/、C:/)规避路径解析坑。
export JAVA_HOME="C:/Program Files/Java/jdk-17"
MAVEN_HOME="D:/apache-maven-3.6.3"
PROJ_WIN="C:/Users/23030/WorkBuddy/2026-08-10-17-16-08/campus-recruit-ai"
exec "$JAVA_HOME/bin/java" \
  -classpath "$MAVEN_HOME/boot/plexus-classworlds-2.6.0.jar" \
  "-Dclassworlds.conf=$MAVEN_HOME/bin/m2.conf" \
  "-Dmaven.home=$MAVEN_HOME" \
  "-Dlibrary.jansi.path=$MAVEN_HOME/lib/jansi-native" \
  "-Dmaven.multiModuleProjectDirectory=$PROJ_WIN" \
  org.codehaus.plexus.classworlds.launcher.Launcher "$@"
