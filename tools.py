"""工具层：把后端接口封装成LLM可调用的工具（docstring就是给LLM的说明书）"""
from langchain_core.tools import tool

from client import client


# ==================== 查询类工具 ====================

@tool
def query_products(name: str = "", page: int = 0, size: int = 10) -> str:
    """按名称查询商品列表。name为名称关键字，可为空表示查全部；page从0开始。
    返回商品的id、sku、名称、价格、低库存阈值。
    注意：操作商品前先用它拿到商品id。"""
    return client.call("GET", "/products", params={"name": name, "page": page, "size": size})


@tool
def query_inventory(keyword: str = "", low_stock_only: bool = False) -> str:
    """查询库存。keyword为商品名关键字；low_stock_only=True时只返回低库存商品。
    返回总量(quantity)、已预留(reservedQuantity)、可售数量(availableQuantity)和状态(NORMAL/LOW_STOCK)。"""
    return client.call("GET", "/inventory",
                       params={"keyword": keyword, "lowStockOnly": low_stock_only})


@tool
def query_orders(type: str = "", status: str = "") -> str:
    """查询订单列表。type可选INBOUND(入库)/OUTBOUND(出库)；
    status可选CREATED(已创建待确认)、RESERVED(已预占)、CONFIRMING(确认中)、CONFIRMED(已完成)、CANCELLED(已取消)。
    确认或取消订单前先用它找到订单id。"""
    return client.call("GET", "/orders", params={"type": type, "status": status})


@tool
def get_dashboard_summary() -> str:
    """获取看板汇总：商品总数、库存总量、低库存商品数、待处理入库单数、待处理出库单数。
    用户问"整体情况/概览"时使用。"""
    return client.call("GET", "/dashboard/summary")


# ==================== 商品增删改工具 ====================

@tool
def create_product(name: str, price: float, sku: str = "", category: str = "",
                   unit: str = "", low_stock_threshold: int = 0) -> str:
    """新增商品。name(名称)和price(价格)必填，其余可选。
    low_stock_threshold为低库存预警阈值。"""
    return client.call("POST", "/products", json={
        "name": name, "price": price, "sku": sku, "category": category,
        "unit": unit, "lowStockThreshold": low_stock_threshold})


@tool
def update_product(product_id: int, name: str, price: float, sku: str = "",
                   category: str = "", unit: str = "", low_stock_threshold: int = 0) -> str:
    """修改商品信息（全字段覆盖式更新）。需要先通过query_products拿到商品id。"""
    return client.call("PUT", f"/products/{product_id}", json={
        "name": name, "price": price, "sku": sku, "category": category,
        "unit": unit, "lowStockThreshold": low_stock_threshold})


@tool
def delete_product(product_id: int) -> str:
    """删除商品（仅允许库存为0且无未完成单据的商品）。
    这是不可逆操作，必须先经用户明确确认。"""
    return client.call("DELETE", f"/products/{product_id}")


# ==================== 库存调整工具 ====================

@tool
def adjust_inventory(product_id: int, quantity: int, remark: str = "") -> str:
    """调整某商品库存。quantity为正数=增加库存，负数=扣减库存，不能为0。
    例：'入库50件'→quantity=50；'扣减10件'→quantity=-10。
    扣减前建议先查询当前库存；这是写操作，必须先经用户明确确认。"""
    return client.call("POST", "/inventory/adjust",
                       json={"productId": product_id, "quantity": quantity, "remark": remark})


# ==================== 订单工具 ====================

@tool
def create_inbound_order(items: list[dict], remark: str = "") -> str:
    """创建入库单（仅建单暂不影响库存，需再调confirm_inbound_order确认后才真正入库）。
    items为列表格式：[{"productId": 1, "quantity": 10}]"""
    return client.call("POST", "/orders/inbound", json={"items": items, "remark": remark})


@tool
def confirm_inbound_order(order_id: int) -> str:
    """确认入库单，库存将通过消息队列异步增加。写操作，必须先经用户明确确认。"""
    return client.call("POST", f"/orders/inbound/{order_id}/confirm")


@tool
def create_outbound_order(items: list[dict], remark: str = "") -> str:
    """创建出库单（立即预占库存，可用库存不足会失败；需再调confirm_outbound_order确认后才真正扣减）。
    items为列表格式：[{"productId": 1, "quantity": 5}]"""
    return client.call("POST", "/orders/outbound", json={"items": items, "remark": remark})


@tool
def confirm_outbound_order(order_id: int) -> str:
    """确认出库单，库存将通过消息队列异步扣减。写操作，必须先经用户明确确认。"""
    return client.call("POST", f"/orders/outbound/{order_id}/confirm")


@tool
def cancel_outbound_order(order_id: int) -> str:
    """取消出库单并释放预占库存，仅"已预占(RESERVED)"状态的出库单可取消。"""
    return client.call("POST", f"/orders/outbound/{order_id}/cancel")


# ==================== 工具汇总 ====================

ALL_TOOLS = [
    # 查询
    query_products, query_inventory, query_orders, get_dashboard_summary,
    # 商品增删改
    create_product, update_product, delete_product,
    # 库存调整
    adjust_inventory,
    # 订单
    create_inbound_order, confirm_inbound_order,
    create_outbound_order, confirm_outbound_order, cancel_outbound_order,
]