package com.renard.ocr.documents.creation.ocr

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.renard.ocr.databinding.ItemListPreviewPageBinding
import kotlinx.coroutines.*

internal class PageAdapter : ListAdapter<PageAdapter.Page, PageAdapter.ViewHolder>(ITEM_CALLBACK) {
    internal data class Page(val uri: Uri, val pageNumber: Int)

    companion object {
        val ITEM_CALLBACK = object : DiffUtil.ItemCallback<Page>() {
            override fun areItemsTheSame(oldItem: Page, newItem: Page) =
                    oldItem == newItem

            override fun areContentsTheSame(oldItem: Page, newItem: Page) =
                    areItemsTheSame(oldItem, newItem)
        }
    }

    internal class ViewHolder(private val parent: ViewGroup, private val binding: ItemListPreviewPageBinding) : RecyclerView.ViewHolder(binding.root) {
        private var job: Job? = null
        fun bind(page: Page) {
            val context = binding.root.context
            job?.cancel()
            binding.page.setImageBitmap(null)
            val height = parent.height
            val width = (height / 1.4142).toInt()

            job = CoroutineScope(Dispatchers.IO).launch {
                val bitmap = if (page.uri.isPdf(context.contentResolver)) {
                    getPdfDocument(page.uri, context)?.use {
                        it.getPageAsBitmap(page.pageNumber, width, height)
                    }
                } else {
                    Glide.with(itemView.context)
                            .asBitmap()
                            .load(page.uri)
                            .apply(RequestOptions.skipMemoryCacheOf(true))
                            .submit(width, height).get()
                }
                withContext(Dispatchers.Main) {
                    Glide.with(itemView.context)
                            .asBitmap()
                            .load(bitmap)
                            .transition(BitmapTransitionOptions.withCrossFade(200))
                            .into(binding.page)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListPreviewPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(parent, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}