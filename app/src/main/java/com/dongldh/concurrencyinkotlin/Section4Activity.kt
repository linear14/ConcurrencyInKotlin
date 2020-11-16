package com.dongldh.concurrencyinkotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dongldh.concurrencyinkotlin.adapter.ArticleAdapter
import com.dongldh.concurrencyinkotlin.data.Article
import com.dongldh.concurrencyinkotlin.data.Feed
import kotlinx.android.synthetic.main.activity_section_4.*
import kotlinx.coroutines.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class Section4Activity : AppCompatActivity() {

    private val dispatcher = newFixedThreadPoolContext(2, name = "IO")
    private val factory = DocumentBuilderFactory.newInstance()

    private lateinit var articles: RecyclerView
    private lateinit var viewAdapter: ArticleAdapter

    private val feeds = listOf(
        Feed("npr", "https://www.npr.org/rss/rss.php?id=1001"),
        Feed("cnn", "http://rss.cnn.com/rss/cnn_topstories.rss"),
        Feed("fox", "http://feeds.foxnews.com/foxnews/politics?format=xml"),
        Feed("inv", "htt:myNewsFeed")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_section_4)

        viewAdapter = ArticleAdapter()
        articles = findViewById<RecyclerView>(R.id.articles).apply {
            adapter = viewAdapter
        }

        asyncLoadNews()

    }

    private fun asyncLoadNews(): Job = GlobalScope.launch {
        val requests = mutableListOf<Deferred<List<Article>>>()

        feeds.mapTo(requests) {
            asyncFetchArticles(it, dispatcher)
        }

        requests.forEach {
            it.join()
        }

        val articles: List<Article> = requests
            .filter { !it.isCancelled }
            .flatMap { it.getCompleted() }

        val failed: Int = requests
            .filter { it.isCancelled }
            .size

        val obtained = requests.size - failed

        GlobalScope.launch(Dispatchers.Main) {
            progress_bar.visibility = View.GONE
            viewAdapter.add(articles)
        }
    }


    private fun asyncFetchArticles(feed: Feed, dispatcher: CoroutineDispatcher): Deferred<List<Article>> =
        GlobalScope.async(dispatcher) {
            delay(1000)
            val builder = factory.newDocumentBuilder()
            val xml = builder.parse(feed.url)
            val news = xml.getElementsByTagName("channel").item(0)

            (0 until news.childNodes.length)
                .map { news.childNodes.item(it) }
                .filter { Node.ELEMENT_NODE == it.nodeType }
                .map { it as Element }
                .filter { "item" == it.tagName }
                .map {
                    val title = it.getElementsByTagName("title").item(0).textContent
                    var summary = it.getElementsByTagName("description").item(0).textContent
                    if(summary.contains("<div")) {
                        summary = summary.substring(0, summary.indexOf("<div"))
                    }
                    Article(feed.name, title, summary)
                }
        }
}