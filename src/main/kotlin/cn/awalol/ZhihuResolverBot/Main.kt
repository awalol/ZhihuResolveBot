import cn.awalol.ZhihuResolverBot.bean.ConfigBean
import cn.awalol.ZhihuResolverBot.bean.ElementBean
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONWriter
import com.alibaba.fastjson2.to
import com.alibaba.fastjson2.toJSONString
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.system.exitProcess

val logger = LoggerFactory.getLogger("ZhihuResolverBot")
val config = loadConfig()

suspend fun main() {
    val proxyHost = System.getProperty("http.proxyHost")
    val proxyPort = System.getProperty("http.proxyPort")
    val bot = telegramBot(config.botToken){
        engine {
            if(!proxyHost.isNullOrEmpty() && !proxyPort.isNullOrEmpty()){
                proxy = Proxy(Proxy.Type.HTTP,InetSocketAddress(proxyHost,proxyPort.toInt()))
            }
        }
    }

    val client = HttpClient(CIO){
        engine {
            if(!proxyHost.isNullOrEmpty() && !proxyPort.isNullOrEmpty()){
                proxy = Proxy(Proxy.Type.HTTP,InetSocketAddress(proxyHost,proxyPort.toInt()))
            }
        }
        buildHeaders {
            set(HttpHeaders.UserAgent,"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.63 Safari/537.36 Edg/102.0.1245.30")
        }
    }

    bot.buildBehaviourWithLongPolling {
        logger.info("启动TG机器人")
        logger.debug(getMe().toString())

        onContentMessage {
            logger.debug(it.toString())
            val url = (it.content as TextContent).textSources[0].source
            logger.debug("Get Source : $url")
            val zhihuContent = client.get(url){
                headers.append(HttpHeaders.Cookie,config.cookie)
            }.body<String>()
            logger.debug(zhihuContent)
            val jsoup = Jsoup.parse(zhihuContent)
            val content = getContent(jsoup, url).plus("<br><a href=\"$url\">文章来源</a></br>")
            val title = jsoup.title()
            if(config.telegraphToken.isNotEmpty()){
                try {
                    this.sendMessage(it.chat.id,telegraph(content, title, client,url))
                }catch (e : Exception){
                    this.sendMessage(it.chat.id,e.localizedMessage!!)
                }
            }
            if(config.yuqueToken.isNotEmpty() && config.namespace.isNotEmpty()){
                try {
                    this.sendMessage(it.chat.id,yuque(client, title, content))
                }catch (e : Exception){
                    this.sendMessage(it.chat.id,e.message!!)
                }
            }
        }

    }.join()
}

fun loadConfig(): ConfigBean {
    logger.info("加载配置")
    val file = File("config.json")
    if (!file.exists() || !file.isFile) {
        logger.error("初始化配置")
        val config = ConfigBean().toJSONString(JSONWriter.Feature.PrettyFormat)
        //写入配置
        val fw = FileWriter(file)
        fw.write(config)
        fw.close()
        exitProcess(1)
    }
    //读取配置内容
    return FileReader(file).readText().to()
}

suspend fun telegraph(content: String, title: String, client: HttpClient,source: String) : String{
    val filterTags = listOf("p","img","blockquote","strong")
    val answerHtml = Jsoup.parse(content)
    val result = JSONArray()
    for (element in answerHtml.allElements) {
        if(filterTags.contains(element.tagName())){
            val abc = when(element.tagName()){
                "p" -> {
                    if(!element.hasAttr("strong")){
                        ElementBean(
                            tag = "p",
                            children = listOf(element.text())
                        )
                    }else{
                        continue
                    }
                }
                "img" -> {
                    val src = element.attr("src")
                    if(!src.contains("data:image")){
                        ElementBean(
                            tag = "figure",
                            children = listOf(
                                ElementBean(
                                    tag = "div",
                                    attrs = JSONObject.of("class","figure_wrapper"),
                                    children = listOf(ElementBean(
                                        tag = "img",
                                        attrs = JSONObject.of("src",src)
                                    ))
                                ),
                                ElementBean(
                                    tag = "figcaption"
                                )
                            )
                        )
                    } else {
                        continue
                    }
                }
                "blockquote" -> ElementBean(
                    tag = "blockquote",
                    children = listOf(element.text())
                )
                "strong" -> ElementBean(
                    tag = "p",
                    children = listOf(ElementBean(
                        tag = "strong",
                        children = listOf(element.text())
                    ))
                )
                else -> {null}
            }
            if (abc != null) {
                result.add(abc)
            }
        }
    }

    val json = result.toJSONString()
    logger.debug(json)

    val publishTelegraph : String = client.post("https://api.telegra.ph/createPage"){
        setBody(MultiPartFormDataContent(
            formData {
                append("access_token",config.telegraphToken)
                append("title",title)
                append("content",json)
                append("author_name","Source")
                append("author_url",source)
            }
        ))
    }.bodyAsText()
    logger.debug(publishTelegraph)
    val telegraphJSONObject = publishTelegraph.to<JSONObject>()

    if(telegraphJSONObject.getBoolean("ok")){
        val telegraphUrl = telegraphJSONObject.getJSONObject("result").getString("url")
        logger.info("$title | $telegraphUrl")
        return if(config.telegraphMirrorHost.isNotEmpty()){
            telegraphUrl.replaceFirst("telegra.ph",config.telegraphMirrorHost)
        }else{
            telegraphUrl
        }
    }else{
        val error = telegraphJSONObject.getString("error")
        logger.error(error)
        throw Exception(error)
    }
}

suspend fun yuque(client: HttpClient, title: String, content: String) : String{
    val requestJson = JSONObject().also {
        it["title"] = title
        it["format"] = "html"
        it["body"] = content
    }

    val response : HttpResponse = client.post("https://www.yuque.com/api/v2/repos/${config.namespace}/docs/"){
        setBody(requestJson.toJSONString())
        headers{
            append(HttpHeaders.ContentType,"application/json")
            append("X-Auth-Token",config.yuqueToken)
            set(HttpHeaders.UserAgent,"ZhihuResolverBot")
        }
    }

    if(response.status == HttpStatusCode.OK){
        val data = response.bodyAsText().to<JSONObject>()
        logger.debug(data.toJSONString(JSONWriter.Feature.PrettyFormat))
        val result = "https://www.yuque.com/${config.namespace}/${data.getJSONObject("data").getString("slug")}"
        logger.info("$title | $result")
        return result
    }else{
        throw Exception(response.bodyAsText())
    }
}

fun getContent(jsoup: Document,url: String) : String{
    if(url.contains("/answer/")){
        val initialData = jsoup.getElementById("js-initialData")!!.text().to<JSONObject>()
        val answer = initialData
            .getJSONObject("initialState")
            .getJSONObject("entities")
            .getJSONObject("answers")
            .getJSONObject(url.split("/").last())
        val content = if(answer.containsKey("paidInfo")) {
            answer.getJSONObject("paidInfo").getString("content")
        } else{
            answer.getString("content")
        }
        return content
    }else {
        val resolved = jsoup.getElementById("resolved")!!.text().to<JSONObject>()
        return resolved
            .getJSONObject("appContext")
            .getJSONObject("__connectedAutoFetch")
            .getJSONObject("manuscript")
            .getJSONObject("data")
            .getJSONObject("manuscriptData")
            .getString("manuscript")
    }
}