"""表现层：对外契约 POST /api/ai/chat（响应结构与Java端Result保持一致）"""
import uuid

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from agent import chat

app = FastAPI(title="Warehouse AI Agent")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境收紧为前端域名
    allow_methods=["*"],
    allow_headers=["*"],
)

OFFLINE_REPLY = "AI 客服暂时离线，请稍后再试，或联系人工客服。"


class ChatRequest(BaseModel):
    sessionId: str = ""
    message: str = ""


class ChatData(BaseModel):
    sessionId: str
    reply: str
    offline: bool
    toolUsed: str | None = None


def result_ok(data: dict) -> dict:
    """与Java侧Result一致：code=0 成功"""
    return {"code": 0, "message": "ok", "data": data}


def result_fail(message: str) -> dict:
    return {"code": -1, "message": message, "data": None}


@app.post("/api/ai/chat")
def chat_api(req: ChatRequest):
    # 1.参数校验（与Java侧行为对齐）
    if not req.message.strip():
        return result_fail("消息不能为空")
    # 2.sessionId为空则生成，保证响应始终带会话标识
    sid = req.sessionId or str(uuid.uuid4())
    # 3.调用Agent；任何异常都离线降级，保证接口始终成功响应
    try:
        reply, tool_used = chat(req.message, sid)
        data = ChatData(sessionId=sid, reply=reply, offline=False, toolUsed=tool_used)
    except Exception as e:
        print(f"[AI] chat error: {e}")
        data = ChatData(sessionId=sid, reply=OFFLINE_REPLY, offline=True, toolUsed=None)
    return result_ok(data.model_dump())


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8001)