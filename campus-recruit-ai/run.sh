#!/usr/bin/env bash
# 本沙箱里启动后端的一键脚本。
#
# 为什么需要它：
# 1) 沙箱 Git Bash 缺 cygpath，mvn 脚本会因路径风格问题报错 → 用 Java 直接拉起 Maven 启动器。
# 2) 沙箱环境自带环境变量 SERVER__PORT=0，会被 Spring Boot 宽松绑定成 server.port=0，
#    导致 Tomcat 随机端口启动（不是配置的 8080）→ 这里用 env -u 排除掉它。
#
# 用法： ./run.sh
set -e
export JAVA_HOME="C:/Program Files/Java/jdk-17"
MAVEN_HOME="D:/apache-maven-3.6.3"
PROJ="C:/Users/23030/WorkBuddy/2026-08-10-17-16-08/campus-recruit-ai"

cd "$PROJ"

# 从 .env 读取 DEEPSEEK_API_KEY（如果存在）。
# 为什么要处理得这么啰嗦？因为 Windows 记事本保存的文件是 CRLF 换行，
# 直接 source 进来变量的值末尾会带一个不可见的 \r，导致发给 AI 的 Key 变成 "sk-xxx\r"，
# 服务端认为 Key 无效报 401 —— 这是非常隐蔽的坑，所以这里统一把 \r 和首尾空格清掉。
if [ -f "$PROJ/.env" ]; then
  KEY_LINE=$(grep -E '^DEEPSEEK_API_KEY=' "$PROJ/.env" | head -1)
  if [ -n "$KEY_LINE" ]; then
    export DEEPSEEK_API_KEY=$(echo "$KEY_LINE" | cut -d= -f2- | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
  fi
fi

if [ -n "$DEEPSEEK_API_KEY" ]; then
  echo "[启动] 已从 .env 读取 DEEPSEEK_API_KEY（长度 ${#DEEPSEEK_API_KEY}）"
else
  echo "[启动] 未检测到 DEEPSEEK_API_KEY —— AI 相关接口会返回友好提示，其它功能正常。"
  echo "       如需启用 AI：把 Key 填到 $PROJ/.env 的 DEEPSEEK_API_KEY= 后面，再重新运行本脚本。"
fi

env -u SERVER__PORT "$JAVA_HOME/bin/java" \
  -classpath "$MAVEN_HOME/boot/plexus-classworlds-2.6.0.jar" \
  "-Dclassworlds.conf=$MAVEN_HOME/bin/m2.conf" \
  "-Dmaven.home=$MAVEN_HOME" \
  "-Dlibrary.jansi.path=$MAVEN_HOME/lib/jansi-native" \
  "-Dmaven.multiModuleProjectDirectory=$PROJ" \
  org.codehaus.plexus.classworlds.launcher.Launcher spring-boot:run
