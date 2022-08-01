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
import io.ktor.util.*
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.system.exitProcess

val logger = LoggerFactory.getLogger("ZhihuResolverBot")
val config = loadConfig()

@OptIn(InternalAPI::class)
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
        buildHeaders {
            this.append(HttpHeaders.UserAgent,"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.63 Safari/537.36 Edg/102.0.1245.30")
        }
        engine {
            if(!proxyHost.isNullOrEmpty() && !proxyPort.isNullOrEmpty()){
                proxy = Proxy(Proxy.Type.HTTP,InetSocketAddress(proxyHost,proxyPort.toInt()))
            }
        }
    }

    bot.buildBehaviourWithLongPolling {
        logger.info("启动TG机器人")
        logger.debug(getMe().toString())

        onContentMessage {
            logger.debug(it.toString())
            val url = (it.content as TextContent).textSources.first().source
            logger.debug("Get Source : $url")
            val zhihuContent = client.get(url){
                headers.append(HttpHeaders.Cookie,config.cookie)
            }.body<String>()
            val jsoup = Jsoup.parse(zhihuContent)
            val initialData = jsoup.getElementById("js-initialData")!!.html().to<JSONObject>()
//            logger.debug(initialData.toJSONString())
            val answer = initialData.getJSONObject("initialState")
                .getJSONObject("entities")
                .getJSONObject("answers")
                .getJSONObject(url.split("/").last())
            val content = if(answer.containsKey("paidInfo")) {
                answer.getJSONObject("paidInfo").getString("content")
            } else{
                answer.getString("content")
            }
//            logger.debug(answer)
            val filterTags = listOf("p","img","blockquote","strong")
            val answerHtml = Jsoup.parse(content)
            val result = JSONArray()
            for (element in answerHtml.allElements) {
                if(filterTags.contains(element.tagName())){
                    val abc = when(element.tagName()){
                        "p" -> ElementBean(
                            tag = "p",
                            children = listOf(element.text())
                        )
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
            //在末尾添加文章来源
            result.add(ElementBean(
                tag = "p",
                children = listOf("文章来源: $url")
            ))

            val json = result.toJSONString()
            logger.debug(json)

            val publishTelegraph : String = client.post("https://api.telegra.ph/createPage"){
                body=MultiPartFormDataContent(
                    formData {
                        append("access_token",config.telegraphToken)
                        append("title",jsoup.title().substring(0,jsoup.title().length - 5))
                        append("content",json)
                    }
                )
            }.bodyAsText()
            logger.debug(publishTelegraph)
            val telegraphJSONObject = publishTelegraph.to<JSONObject>()

            if(telegraphJSONObject.getBoolean("ok")){
                val telegraphUrl = telegraphJSONObject.getJSONObject("result").getString("url")
                logger.info("${jsoup.title()} | $telegraphUrl")
                this.sendMessage(it.chat.id,telegraphUrl)
            }else{
                val error = telegraphJSONObject.getString("error")
                logger.error(error)
                this.sendMessage(it.chat.id,"Error: $error")
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
