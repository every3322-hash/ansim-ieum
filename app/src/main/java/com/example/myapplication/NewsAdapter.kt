import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.NewsItem
import com.example.myapplication.R

class NewsAdapter(private val context: Context, private val newsList: List<NewsItem>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val content: TextView = view.findViewById(R.id.tvContent)
        val date: TextView = view.findViewById(R.id.tvDate)
        val writer: TextView = view.findViewById(R.id.tvWriter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val newsItem = newsList[position]
        holder.title.text = newsItem.title
        holder.content.text = newsItem.content
        holder.date.text = newsItem.date
        holder.writer.text = newsItem.writer

        // 기사 클릭 시 연합뉴스 링크로 이동
        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.link))
            context.startActivity(intent)
        }
    }



    fun updateData(newData: List<NewsItem>) {
        (newsList as MutableList).clear()
        (newsList as MutableList).addAll(newData)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = newsList.size
}