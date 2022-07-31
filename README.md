# 知乎机器人

## 用处

获取知乎内的回答并做成Telegraph支持在Telegram内阅读

## 使用方法

- 初次启动机器人会生成一个`config.json`的配置文件

``` json
{
"botToken":"填入从BotFather生成的token（用于在Telegram接收和发送链接）",
"cookie":"知乎的Cookie（可选）",
"telegraphToken":"Telegraph账号的`access_token`"
}
```

- 启动机器人后在Telegram给机器人发送知乎问答链接，之后机器人会返回Telegraph链接
