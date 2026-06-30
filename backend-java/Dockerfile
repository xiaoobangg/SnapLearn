# ============================================
# SnapLearn Backend - Dockerfile
# 本地 mvn package 打好 JAR 后再 docker build
# ============================================

FROM openjdk:17-jdk-slim
WORKDIR /app

# 创建上传目录
RUN mkdir -p /app/uploads

# 复制本地构建好的 JAR（需要先执行 mvn clean package -DskipTests）
COPY target/snaplearn.jar app.jar

# Arthas 诊断工具
COPY arthas-packaging-4.3.0-bin /app/arthas

EXPOSE 8080

ENV JAVA_OPTS="-Xmx256m -Xms128m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
