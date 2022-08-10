# 知乎机器人

## 使用方法

- 初次启动机器人会生成一个`config.json`的配置文件

``` json
{
    "namespace":"语雀namespace",
    "telegraphMirrorHost":"telegraph镜像站Host",
    "yuqueToken":"语雀Token",
    "botToken":"填入从BotFather生成的token（用于在Telegram接收和发送链接）",
    "cookie":"知乎的Cookie（可选）",
    "telegraphToken":"Telegraph账号的 access_token"
}
```

- 启动机器人后在Telegram给机器人发送知乎问答链接，之后机器人会返回Telegraph链接
- 当 `telegraphToken` 和 `yuqueToken` 都同时填写，则两个平台都会发
- 'telegraphToken' 与 'yuqueToken' 任选一个填写即可
- telegraph文章大小限制为64kb，语雀为5MB，太长的文章可能会发送失败