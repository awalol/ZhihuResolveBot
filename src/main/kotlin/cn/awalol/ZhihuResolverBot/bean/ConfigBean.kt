package cn.awalol.ZhihuResolverBot.bean

import com.alibaba.fastjson2.annotation.JSONField

data class ConfigBean(
    @JSONField(name = "botToken")
    val botToken : String= "token",
    @JSONField(name = "cookie")
    val cookie : String = "",
    @JSONField(name = "telegraphToken")
    val telegraphToken : String = "",
    @JSONField(name = "telegraphMirrorHost")
    val telegraphMirrorHost : String = "",
    @JSONField(name = "yuqueToken")
    val yuqueToken : String = "",
    @JSONField(name = "namespace")
    val namespace : String = ""
)
