#!/bin/bash
# ============================================
# SnapLearn 服务器端重启脚本
# 用法:
#   ./restart-server.sh                # 重启所有服务（保留容器、不重建）
#   ./restart-server.sh backend        # 只重启 backend
#   ./restart-server.sh admin          # 只重启 admin
#   ./restart-server.sh prometheus     # 只重启 Prometheus
#   ./restart-server.sh grafana        # 只重启 Grafana
#   ./restart-server.sh recreate       # down 然后 up，彻底重建容器（保留 volume 数据）
#   ./restart-server.sh logs <服务名>   # 跟踪某服务最近日志（如 logs backend）
# ============================================
set -e

# ---- 配置 ----
PROJECT_DIR="/home/SnapLearn"
COMPOSE_FILE="docker-compose.server.yml"
ENV_FILE=".env.server"

cd "${PROJECT_DIR}"

ACTION="${1:-restart-all}"
SERVICE="${2:-}"

case "${ACTION}" in
    restart-all|"")
        echo "==> 重启所有服务（restart 模式，保留容器）"
        docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" restart
        ;;

    backend|admin|prometheus|grafana)
        echo "==> 重启服务: ${ACTION}"
        docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" restart "${ACTION}"
        ;;

    recreate)
        echo "==> 彻底重建：down → up（volume 数据保留）"
        docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" down
        docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --remove-orphans
        ;;

    logs)
        if [ -z "${SERVICE}" ]; then
            echo "Usage: $0 logs <backend|admin|prometheus|grafana>"
            exit 1
        fi
        docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" logs -f --tail=100 "${SERVICE}"
        exit 0
        ;;

    *)
        echo "未知参数: ${ACTION}"
        echo ""
        echo "用法:"
        echo "  $0                       重启所有服务"
        echo "  $0 backend               重启 backend"
        echo "  $0 admin                 重启 admin"
        echo "  $0 prometheus            重启 Prometheus"
        echo "  $0 grafana               重启 Grafana"
        echo "  $0 recreate              彻底重建所有容器（保留 volume）"
        echo "  $0 logs <service>        查看某服务日志"
        exit 1
        ;;
esac

# ---- 健康检查 ----
echo ""
echo "==> 等待 5 秒后检查容器状态..."
sleep 5

echo ""
echo "容器状态:"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps

echo ""
HOST_IP=$(hostname -I 2>/dev/null | awk '{print $1}')

# 仅在重启了 backend 或 全部 / recreate 时检查 backend
if [ "${ACTION}" = "backend" ] || [ "${ACTION}" = "restart-all" ] || [ "${ACTION}" = "" ] || [ "${ACTION}" = "recreate" ]; then
    echo "后端健康检查:"
    curl -s -m 5 http://localhost:8080/api/health 2>/dev/null && echo "" || echo "(后端仍在启动中)"
fi

if [ "${ACTION}" = "prometheus" ] || [ "${ACTION}" = "restart-all" ] || [ "${ACTION}" = "" ] || [ "${ACTION}" = "recreate" ]; then
    echo "Prometheus 健康检查:"
    curl -s -m 5 http://localhost:9090/-/healthy 2>/dev/null && echo "" || echo "(Prometheus 仍在启动中)"
fi

if [ "${ACTION}" = "grafana" ] || [ "${ACTION}" = "restart-all" ] || [ "${ACTION}" = "" ] || [ "${ACTION}" = "recreate" ]; then
    echo "Grafana 健康检查:"
    curl -s -m 5 http://localhost:3100/api/health 2>/dev/null && echo "" || echo "(Grafana 仍在启动中)"
fi

echo ""
echo "=========================================="
echo " 重启完成"
echo " 后端 API:      http://${HOST_IP}:8080"
echo " 管理后台:      http://${HOST_IP}:3001"
echo " Prometheus:    http://${HOST_IP}:9090"
echo " Grafana:       http://${HOST_IP}:3100"
echo "=========================================="
