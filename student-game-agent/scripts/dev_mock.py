"""启动独立的本地 mock API 和学生前端，Ctrl+C 停止；不修改真实 .env。"""

import argparse
import json
import os
import secrets
import signal
import socket
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WEB = ROOT.parent / "student-web"
PLANET = ROOT.parent / "gamesoptionproject"


def ensure_free(port):
    with socket.socket() as probe:
        try:
            probe.bind(("127.0.0.1", port))
        except OSError as exc:
            raise SystemExit(
                f"端口 {port} 已被占用，请使用 --api-port 或 --web-port 选择其他端口。"
            ) from exc


def mock_environment():
    folder = ROOT / "data/mock"
    folder.mkdir(parents=True, exist_ok=True)
    key_file = folder / "keys.json"
    if not key_file.exists():
        with os.fdopen(
            os.open(key_file, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w"
        ) as output:
            json.dump({"service": secrets.token_hex(32), "jwt": secrets.token_hex(32)}, output)
    keys = json.loads(key_file.read_text())
    return {
        **os.environ,
        "AGENT_ENVIRONMENT": "development",
        "AGENT_MOCK_MODE": "true",
        "AGENT_SERVICE_API_KEY": keys["service"],
        "AGENT_JWT_SECRET": keys["jwt"],
        "AGENT_DATABASE_URL": "sqlite+aiosqlite:///" + str(folder / "agent.db"),
        "AGENT_BASE_PROMPT_KEY": "companion.base",
        "AGENT_BASE_PROMPT_VERSION": "1.0.0",
        "AGENT_CHAT_PROMPT_KEY": "companion.chat",
        "AGENT_CHAT_PROMPT_VERSION": "1.0.0",
        "AGENT_EVALUATION_PROMPT_KEY": "companion.evaluation",
        "AGENT_EVALUATION_PROMPT_VERSION": "1.0.0",
        "AGENT_RAG_ENABLED": "false",
        "AGENT_MOCK_WORKER_ENABLED": "true",
        "LANGSMITH_TRACING": "false",
        "LANGCHAIN_TRACING_V2": "false",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-port", type=int, default=8088)
    parser.add_argument("--web-port", type=int, default=5175)
    parser.add_argument("--api-only", action="store_true")
    parser.add_argument("--with-planet", action="store_true")
    parser.add_argument("--planet-port", type=int, default=5177)
    args = parser.parse_args()
    if args.api_only and args.with_planet:
        parser.error("--api-only 与 --with-planet 不能同时使用")
    ensure_free(args.api_port)
    if not args.api_only:
        ensure_free(args.web_port)
        if not (WEB / "node_modules").is_dir():
            raise SystemExit("请先在 student-web 执行 npm ci。")
    if args.with_planet:
        ensure_free(args.planet_port)
        if not (PLANET / "node_modules").is_dir():
            raise SystemExit("请先在 gamesoptionproject 执行 npm ci。")
    env = mock_environment()
    subprocess.run(
        [sys.executable, "-m", "alembic", "upgrade", "head"], cwd=ROOT, env=env, check=True
    )
    children = []

    def stop(_signum, _frame):
        raise KeyboardInterrupt

    signal.signal(signal.SIGTERM, stop)
    try:
        api = subprocess.Popen(
            [
                sys.executable,
                "-m",
                "uvicorn",
                "src.app.main:create_app",
                "--factory",
                "--host",
                "127.0.0.1",
                "--port",
                str(args.api_port),
            ],
            cwd=ROOT,
            env=env,
        )
        children.append(api)
        for _ in range(100):
            if api.poll() is not None:
                raise SystemExit("Mock API 启动失败，请查看上方日志。")
            try:
                with urllib.request.urlopen(
                    f"http://127.0.0.1:{args.api_port}/health/ready", timeout=1
                ):
                    break
            except OSError:
                time.sleep(0.1)
        else:
            raise SystemExit("等待 Mock API 启动超时。")
        if not args.api_only:
            children.append(
                subprocess.Popen(
                    ["npm", "run", "dev:mock", "--", "--port", str(args.web_port)],
                    cwd=WEB,
                    env={
                        **env,
                        "MOCK_AGENT_API_TARGET": f"http://127.0.0.1:{args.api_port}",
                        "VITE_WIDGET_PARENT_ORIGINS": f"http://127.0.0.1:{args.planet_port},http://localhost:{args.planet_port}",
                    },
                )
            )
        if args.with_planet:
            children.append(
                subprocess.Popen(
                    ["npm", "run", "dev:mock", "--", "--port", str(args.planet_port)],
                    cwd=PLANET,
                    env={
                        **env,
                        "VITE_AGENT_WIDGET_URL": f"http://127.0.0.1:{args.web_port}/widget",
                    },
                )
            )
            print(f"星球模拟联调: http://127.0.0.1:{args.planet_port}/", flush=True)
        print(f"\nMock API: http://127.0.0.1:{args.api_port}/docs", flush=True)
        if not args.api_only:
            print(f"学生端: http://127.0.0.1:{args.web_port}/login", flush=True)
        print("联调账号: 学校 11000 / 学号 demo / 密码 demo123；Ctrl+C 停止。", flush=True)
        while all(process.poll() is None for process in children):
            time.sleep(0.5)
    except KeyboardInterrupt:
        pass
    finally:
        for process in reversed(children):
            if process.poll() is None:
                process.terminate()
        for process in children:
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


if __name__ == "__main__":
    main()
