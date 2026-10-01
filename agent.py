"""智能层：LLM + Agent + 多轮记忆 + 行为规则"""
import os
import uuid

from langchain.agents import create_agent
from langchain_openai import ChatOpenAI
from langgraph.checkpoint.memory import InMemorySaver

from tools import ALL_TOOLS

model = ChatOpenAI(
    model=os.getenv("MODEL_NAME", "deepseek-chat"),
    api_key=os.getenv("MODEL_API_KEY", "********"),
    base_url=os.getenv("MODEL_BASE_URL", "https://api.deepseek.com"),
    temperature=0,  # 工具调用场景要确定性，不要发挥
)

SYSTEM_PROMPT = """你是仓储管理系统的AI客服助手，通过工具完成用户的查询与操作请求。

规则：
1. 涉及库存、商品、订单的问题，必须先用查询工具拿到真实数据，禁止编造。
2. 写操作（调整库存/删除商品/确认订单）前，必须向用户复述"将要对什么做什么"，得到明确同意后才能调用。
3. 工具返回"操作失败：xxx"时，把原因原样转述给用户。
4. 操作成功后简洁汇报：单号、商品、数量等关键结果。
5. 全程使用中文回答。"""

agent = create_agent(
    model=model,
    tools=ALL_TOOLS,
    system_prompt=SYSTEM_PROMPT,
    checkpointer=InMemorySaver(),  # 多轮会话记忆（重启丢失，升级可用SqliteSaver）
)


def _collect_tool_names(messages) -> str | None:
    """提取本轮Agent调用过的工具名（多个逗号分隔），供契约的toolUsed字段回传"""
    names = []
    for msg in messages:
        for call in getattr(msg, "tool_calls", None) or []:
            names.append(call["name"])
    return ",".join(names) if names else None


def chat(message: str, session_id: str) -> tuple[str, str | None]:
    """执行一轮对话，返回 (回复文本, 使用的工具名)"""
    # sessionId -> 确定性UUID：同一会话恒定映射同一thread_id，实现多轮记忆
    thread_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, session_id))
    result = agent.invoke(
        {"messages": [{"role": "user", "content": message}]},
        config={"configurable": {"thread_id": thread_id}},
    )
    messages = result["messages"]
    return messages[-1].content, _collect_tool_names(messages)
