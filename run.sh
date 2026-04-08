#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# run.sh  –  Start (or stop) the Neo4j + GDS Docker container.
#
# Usage:
#   ./run.sh           – start in the foreground (Ctrl-C to stop)
#   ./run.sh -d        – start in the background (detached)
#   ./run.sh --stop    – stop and remove the container
#   ./run.sh --logs    – tail container logs (when running detached)
# ---------------------------------------------------------------------------
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DETACH=false
ACTION="start"

for arg in "$@"; do
  case "$arg" in
    --stop)  ACTION="stop" ;;
    --logs)  ACTION="logs" ;;
    -d)      DETACH=true ;;
    *)
      echo -e "${RED}Unknown argument: $arg${NC}" >&2
      echo "Usage: $0 [-d] [--stop] [--logs]" >&2
      exit 1
      ;;
  esac
done

if [[ "$ACTION" == "stop" ]]; then
  echo "Stopping Neo4j container..."
  docker compose down
  echo -e "${GREEN}Stopped.${NC}"
  exit 0
fi

if [[ "$ACTION" == "logs" ]]; then
  docker compose logs -f
  exit 0
fi

if ! command -v docker &>/dev/null; then
  echo -e "${RED}Error: 'docker' not found on PATH. Install Docker first.${NC}" >&2
  exit 1
fi

if ! docker info &>/dev/null; then
  echo -e "${RED}Error: Docker daemon is not running.${NC}" >&2
  exit 1
fi

echo "Preparing host directories..."
mkdir -p logs output plugins conf

if [[ -f user-logs.xml ]]; then
  cp user-logs.xml conf/user-logs.xml
  echo "  Copied user-logs.xml -> conf/"
else
  echo -e "${YELLOW}  Warning: user-logs.xml not found – skipping logging config.${NC}"
fi

JAR="target/master-procedures-0.0.1.jar"
if [[ -f "$JAR" ]]; then
  cp "$JAR" plugins/
  echo "  Copied $JAR -> plugins/"
else
  echo -e "${RED}Error: $JAR not found.${NC}" >&2
  echo "  Build the project first:"
  echo "    ./mvnw clean package -DskipTests"
  exit 1
fi

echo ""
echo -e "${GREEN}Starting Neo4j 2025.10.1 with GDS...${NC}"
echo "  Note: GDS will be downloaded on the first run (~100 MB)."
echo "        It is cached in ./plugins/ and skipped on subsequent starts."
echo ""
echo "  Neo4j Browser : http://localhost:7474"
echo "  Bolt URL      : bolt://localhost:7687"
echo "  Logs          : ./logs/"
echo "  Output        : ./output/"
echo ""

if $DETACH; then
  docker compose up -d
  echo -e "${GREEN}Container started in the background.${NC}"
  echo "  View logs : ./run.sh --logs"
  echo "  Stop      : ./run.sh --stop"
else
  echo "Running in the foreground. Press Ctrl-C to stop."
  echo "(Use './run.sh -d' to start detached instead.)"
  echo ""
  docker compose up
fi
