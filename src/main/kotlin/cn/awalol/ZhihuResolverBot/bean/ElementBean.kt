package cn.awalol.ZhihuResolverBot.bean

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.annotation.JSONField

data class ElementBean(
    @JSONField(name = "tag")
    val tag : String = "",
    @JSONField(name = "attrs")
    val attrs : JSONObject = JSONObject.of("dir","auto"),
    @JSONField(name = "children")
    val children : List<Any> = listOf("")
)
