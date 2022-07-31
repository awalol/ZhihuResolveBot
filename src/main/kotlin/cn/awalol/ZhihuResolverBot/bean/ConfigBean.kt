package cn.awalol.ZhihuResolverBot.bean

import com.alibaba.fastjson2.annotation.JSONField

data class ConfigBean(
    @JSONField(name = "botToken")
    val botToken : String= "token",
    @JSONField(name = "cookie")
    val cookie : String = "",
    @JSONField(name = "telegraphToken")
    val telegraphToken : String = ""
)
