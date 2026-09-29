"""通信层：封装对 Spring Boot 后端的 HTTP 调用（登录/token/401重登/错误翻译）"""
import json
import os

import requests

BASE_URL = os.getenv("BACKEND_BASE_URL", "http://localhost:8000")
TIMEOUT = 15  # 单次请求超时（秒）


class WarehouseClient:
    def __init__(self, username: str, password: str):
        self.username = username
        self.password = password
        self.token: str | None = None

    def _login(self):
        """登录获取token（惰性调用，会话失效时会再次调用）"""
        resp = requests.post(
            f"{BASE_URL}/auth/login",
            json={"username": self.username, "password": self.password},
            timeout=5,
        )
        body = resp.json()
        if body.get("code") != 0:
            raise RuntimeError(f"AI账号登录失败: {body.get('message')}")
        self.token = body["data"]["token"]

    def call(self, method: str, path: str, **kwargs) -> str:
        """工具层的统一出口：自动登录、401重登重试、把Result翻译成LLM友好的文本"""
        if self.token is None:
            self._login()
        resp = requests.request(
            method, f"{BASE_URL}{path}",
            headers={"Authorization": self.token}, timeout=TIMEOUT, **kwargs,
        )
        # Redis会话12小时 < JWT 7天，中途会401 → 重新登录再试一次
        if resp.status_code == 401:
            self._login()
            resp = requests.request(
                method, f"{BASE_URL}{path}",
                headers={"Authorization": self.token}, timeout=TIMEOUT, **kwargs,
            )
        body = resp.json()
        if body.get("code") == 0:
            # 成功：只把data给LLM（省token，避免code字段干扰模型判断）
            return json.dumps(body.get("data"), ensure_ascii=False)
        # 失败：转成自然语言，让LLM能理解并转述给用户
        return f"操作失败：{body.get('message')}"


# AI专用账号（需先在数据库user表创建，见文末SQL）
client = WarehouseClient(
    username=os.getenv("AI_ACCOUNT", "ai_agent"),
    password=os.getenv("AI_PASSWORD", "ai_agent_123456"),
)