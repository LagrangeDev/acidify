#!/usr/bin/env python3
"""
测试 Yogurt (acidify) 音乐卡片功能

使用方法:
    python test_music_card.py

配置说明:
    - YOGURT_HOST: Yogurt HTTP 服务地址
    - YOGURT_PORT: Yogurt HTTP 服务端口
    - ACCESS_TOKEN: 访问令牌 (对应 config.json 中的 httpConfig.accessToken)
    - GROUP_ID: 测试群号
"""

import requests
import json

# ============================================================================
# 配置
# ============================================================================

YOGURT_HOST = "127.0.0.1"
YOGURT_PORT = 13000
ACCESS_TOKEN = "dev"

# 测试群号 (改成你自己的群)
GROUP_ID = 259248174

# 测试私聊用户QQ号
USER_ID = 1830540513

# ============================================================================
# HTTP 客户端
# ============================================================================

class YogurtClient:
    def __init__(self, host: str, port: int, token: str):
        self.base_url = f"http://{host}:{port}"
        self.headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}"
        }
    
    def _request(self, endpoint: str, data: dict) -> dict:
        """发送 POST 请求"""
        # 注意: Yogurt 的 API 路由在 /api 前缀下
        url = f"{self.base_url}/api/{endpoint}"
        print(f"\n📤 请求: POST {url}")
        print(f"📦 数据: {json.dumps(data, ensure_ascii=False, indent=2)}")
        
        try:
            resp = requests.post(url, json=data, headers=self.headers, timeout=30)
            print(f"📊 HTTP 状态码: {resp.status_code}")
            print(f"📄 原始响应: {resp.text[:500] if resp.text else '(空)'}")
            
            if resp.text:
                result = resp.json()
                print(f"📥 JSON 响应: {json.dumps(result, ensure_ascii=False, indent=2)}")
                return result
            else:
                return {"status": "failed", "message": "Empty response"}
        except requests.exceptions.RequestException as e:
            print(f"❌ 请求失败: {e}")
            return {"status": "failed", "message": str(e)}
    
    # ========================================================================
    # 音乐卡片 API
    # ========================================================================
    
    def send_group_music(self, group_id: int, music_type: str, **kwargs) -> dict:
        """
        发送群聊音乐卡片
        
        Args:
            group_id: 群号
            music_type: 音乐类型 (qq/163/kugou/migu/kuwo/custom)
            **kwargs: 
                - id: 歌曲ID (平台音乐必填)
                - url: 跳转链接 (custom必填)
                - audio: 音频链接 (custom必填)
                - title: 标题 (custom必填)
                - image: 封面图片 (可选)
                - singer: 歌手 (可选)
        """
        data = {
            "group_id": group_id,
            "music_type": music_type,
            **kwargs
        }
        return self._request("send_group_music", data)
    
    def send_private_music(self, user_id: int, music_type: str, **kwargs) -> dict:
        """发送私聊音乐卡片"""
        data = {
            "user_id": user_id,
            "music_type": music_type,
            **kwargs
        }
        return self._request("send_private_music", data)


# ============================================================================
# 测试用例
# ============================================================================

def test_163_music(client: YogurtClient, group_id: int):
    """测试网易云音乐"""
    print("\n" + "=" * 60)
    print("🎵 测试: 网易云音乐 (163)")
    print("=" * 60)
    
    # 使用你提供的歌曲 ID
    result = client.send_group_music(
        group_id=group_id,
        music_type="163",
        id="1999253939"  # 网易云歌曲ID
    )
    return result


def test_qq_music(client: YogurtClient, group_id: int):
    """测试QQ音乐"""
    print("\n" + "=" * 60)
    print("🎵 测试: QQ音乐")
    print("=" * 60)
    
    result = client.send_group_music(
        group_id=group_id,
        music_type="qq",
        id="384227436"  # QQ音乐歌曲ID
    )
    return result


def test_custom_music(client: YogurtClient, group_id: int):
    """测试自定义音乐卡片"""
    print("\n" + "=" * 60)
    print("🎵 测试: 自定义音乐卡片")
    print("=" * 60)
    
    result = client.send_group_music(
        group_id=group_id,
        music_type="custom",
        url="https://music.163.com/#/song?id=1999253939",
        audio="https://music.163.com/song/media/outer/url?id=1999253939.mp3",
        title="测试自定义音乐",
        image="https://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
        singer="测试歌手"
    )
    return result


def test_kugou_music(client: YogurtClient, group_id: int):
    """测试酷狗音乐"""
    print("\n" + "=" * 60)
    print("🎵 测试: 酷狗音乐")
    print("=" * 60)
    
    # 酷狗音乐需要 hash 作为 ID
    result = client.send_group_music(
        group_id=group_id,
        music_type="kugou",
        id="1571941423D8D7E290E5DD7655E8A7C7"  # 酷狗音乐 hash
    )
    return result


# ============================================================================
# 私聊测试用例
# ============================================================================

def test_private_163_music(client: YogurtClient, user_id: int):
    """测试私聊网易云音乐"""
    print("\n" + "=" * 60)
    print("🎵 私聊测试: 网易云音乐 (163)")
    print("=" * 60)
    
    result = client.send_private_music(
        user_id=user_id,
        music_type="163",
        id="1999253939"
    )
    return result


def test_private_custom_music(client: YogurtClient, user_id: int):
    """测试私聊自定义音乐卡片"""
    print("\n" + "=" * 60)
    print("🎵 私聊测试: 自定义音乐卡片")
    print("=" * 60)
    
    result = client.send_private_music(
        user_id=user_id,
        music_type="custom",
        url="https://music.163.com/#/song?id=1999253939",
        audio="https://music.163.com/song/media/outer/url?id=1999253939.mp3",
        title="私聊测试音乐",
        image="https://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
        singer="测试歌手"
    )
    return result


# ============================================================================
# 主函数
# ============================================================================

def main():
    print("""
╔══════════════════════════════════════════════════════════════╗
║         🍦 Yogurt 音乐卡片测试脚本                           ║
║                                                              ║
║  配置:                                                       ║
║    - 服务地址: {host}:{port}                              ║
║    - 访问令牌: {token}                                       ║
║    - 测试群号: {group}                                     ║
║    - 测试私聊: {user}                                     ║
╚══════════════════════════════════════════════════════════════╝
""".format(
        host=YOGURT_HOST,
        port=YOGURT_PORT,
        token=ACCESS_TOKEN,
        group=GROUP_ID,
        user=USER_ID
    ))
    
    # 创建客户端
    client = YogurtClient(YOGURT_HOST, YOGURT_PORT, ACCESS_TOKEN)
    
    # 选择测试
    print("请选择要测试的音乐类型:")
    print("  === 群聊测试 ===")
    print("  1. 网易云音乐 (163)")
    print("  2. QQ音乐")
    print("  3. 自定义音乐卡片")
    print("  4. 酷狗音乐")
    print("  5. 全部群聊测试")
    print()
    print("  === 私聊测试 ===")
    print("  6. [私聊] 网易云音乐 (163)")
    print("  7. [私聊] 自定义音乐卡片")
    print("  8. [私聊] 全部测试")
    print()
    print("  0. 退出")
    
    while True:
        try:
            choice = input("\n请输入选项 (0-9): ").strip()
            
            if choice == "0":
                print("👋 再见!")
                break
            elif choice == "1":
                test_163_music(client, GROUP_ID)
            elif choice == "2":
                test_qq_music(client, GROUP_ID)
            elif choice == "3":
                test_custom_music(client, GROUP_ID)
            elif choice == "4":
                test_kugou_music(client, GROUP_ID)
            elif choice == "5":
                test_163_music(client, GROUP_ID)
                test_qq_music(client, GROUP_ID)
                test_custom_music(client, GROUP_ID)
            elif choice == "6":
                test_private_163_music(client, USER_ID)
            elif choice == "7":
                test_private_custom_music(client, USER_ID)
            elif choice == "8":
                test_private_163_music(client, USER_ID)
                test_private_custom_music(client, USER_ID)
            else:
                print("❌ 无效选项，请重新输入")
        except KeyboardInterrupt:
            print("\n👋 再见!")
            break


if __name__ == "__main__":
    main()
