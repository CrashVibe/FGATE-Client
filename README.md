# FGATE-Client

FlowGate 新一代的群服互通解决方案（MC服务端侧）

## 配置
```yaml
bind_address: 127.0.0.1
# 我们应该监听哪个地址？
bind_port: 7800
# 我们应将WebSocket服务器绑定到哪个端口？
auth_token: null
# 如果您已设置节点Token，则需要添加验证字段，反之无需添加验证请求头，但是不设置节点Token（设置为null）并不是推荐的做法，除非您的FGate Nexus与节点在同一台设备，并且节点监听`127.0.0.1`,否则您的Minecraft服务器可能面临被攻击的风险
max_token_try_count: 3
# 错误Token的最大尝试次数
```


## ApiDocs

<details><summary>展开</summary>

### 在连接时

当您与节点（即MC服务端侧FGate）建立连接时，FGate会返回一个Hello包

#### Hello包
```json
{
  "id": 102,
  "result": {
    "status": "success",
    "message": "Welcome [您的SessionID] to connect FGate",
    "client_id": "[您的SessionID]"
  }
}
```
#### 登录/验证
请在请求头加入`Authorization`字段，值为`Bearer  [您的节点Token]`，我们将在建立连接时进行验证。

</details>