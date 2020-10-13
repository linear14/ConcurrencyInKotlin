package com.dongldh.concurrencyinkotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_section_2.*
import kotlinx.android.synthetic.main.activity_section_2.news_count
import kotlinx.android.synthetic.main.activity_section_3.*
import kotlinx.coroutines.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class Section3Activity : AppCompatActivity() {

    private val dispatcher = newFixedThreadPoolContext(2, name = "IO")
    private val factory = DocumentBuilderFactory.newInstance()

    val feeds = listOf(
        "https://www.npr.org/rss/rss.php?id=1001",
        "http://rss.cnn.com/rss/cnn_topstories.rss",
        "http://feeds.foxnews.com/foxnews/politics?format=xml",
        "htt:myNewsFeed"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_section_3)

        GlobalScope.launch(dispatcher) {
            asyncLoadNews()
        }

    }

    private fun asyncLoadNews(): Job = GlobalScope.launch {
        val requests = mutableListOf<Deferred<List<String>>>()

        feeds.mapTo(requests) {
            asyncFetchHeadlines(it, dispatcher)
        }

        requests.forEach {
            it.join()
        }

        val headlines: List<String> = requests
            .filter { !it.isCancelled }
            .flatMap { it.getCompleted() }

        val failed: Int = requests
            .filter { it.isCancelled }
            .size

        val obtained = requests.size - failed

        GlobalScope.launch(Dispatchers.Main) {
            news_count.text = "Found ${headlines.size} News in $obtained feeds"

            if(failed > 0) {
                warnings.text = "Failed to fetch $failed feeds"
            }
        }
    }


    private fun asyncFetchHeadlines(feed: String, dispatcher: CoroutineDispatcher): Deferred<List<String>> =
        GlobalScope.async(dispatcher) {
            val builder = factory.newDocumentBuilder()
            val xml = builder.parse(feed)
            val news = xml.getElementsByTagName("channel").item(0)

            (0 until news.childNodes.length)
                .map { news.childNodes.item(it) }
                .filter { Node.ELEMENT_NODE == it.nodeType }
                .map { it as Element }
                .filter { "item" == it.tagName }
                .map { it.getElementsByTagName("title").item(0).textContent }
        }
}