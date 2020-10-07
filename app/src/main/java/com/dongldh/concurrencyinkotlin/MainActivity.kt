package com.dongldh.concurrencyinkotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity() {

    private val netDispatcher = newSingleThreadContext(name = "ServiceCall")
    private val factory = DocumentBuilderFactory.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlobalScope.launch(netDispatcher) {
            loadNews()
        }

        // asyncLoadNews()  // 방식2 사용
    }

    // 방식1. 동기 함수 (사용 시 비동기 호출자로 감싼다)
    private fun loadNews() {
        val headlines = fetchRssHeadlines()
        GlobalScope.launch(Dispatchers.Main) {
            news_count.text = "Found ${headlines.size} News"
        }
    }

    // 방식2. 미리 정의된 디스패처를 가지는 비동기 함수
    private fun asyncLoadNews(): Job = GlobalScope.launch(netDispatcher) {
        val headlines = fetchRssHeadlines()
        GlobalScope.launch(Dispatchers.Main) {
            news_count.text = "Found ${headlines.size} News"
        }
    }

    private fun fetchRssHeadlines(): List<String> {
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse("https://www.npr.org/rss/rss.php?id=1001")
        val news = xml.getElementsByTagName("channel").item(0)
        return (0 until news.childNodes.length)
                .map { news.childNodes.item(it) }
                .filter { Node.ELEMENT_NODE == it.nodeType }
                .map { it as Element }
                .filter { "item" == it.tagName }
                .map { it.getElementsByTagName("title").item(0).textContent }
    }
}