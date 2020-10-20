package com.renard.ocr.main_menu.language

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.NavUtils
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.DividerItemDecoration.HORIZONTAL
import com.renard.ocr.MonitoredActivity
import com.renard.ocr.R
import com.renard.ocr.databinding.ActivityOcrLanguageBinding
import com.renard.ocr.databinding.ItemListOcrLanguageBinding
import com.renard.ocr.databinding.ItemListOcrLanguageBinding.inflate
import com.renard.ocr.main_menu.language.LanguageListViewModel.LoadingState.LOADED
import com.renard.ocr.main_menu.language.LanguageListViewModel.LoadingState.LOADING


class OcrLanguageListActivity : MonitoredActivity(), SearchView.OnQueryTextListener {


    private lateinit var binding: ActivityOcrLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrLanguageBinding.inflate(layoutInflater)

        setContentView(binding.root)
        initToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setToolbarMessage(R.string.ocr_language_title)

        val model: LanguageListViewModel by viewModels()

        val adapter = LanguagesListAdapter {
            if (it.isInstalled) {
                confirmDeleteLanguage(it, model::deleteLanguage)
            } else {
                if (DownloadManagerResolver().resolve(application)) {
                    model.startDownload(it)
                } else {
                    Toast.makeText(
                            this,
                            R.string.no_download_manager,
                            Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        binding.listOcrLanguages.adapter = adapter
        binding.listOcrLanguages.layoutManager = LinearLayoutManager(this)
        binding.listOcrLanguages.setHasFixedSize(true)

        model.loading.observe(this, Observer { it ->
            when (it!!) {
                LOADING -> binding.viewSwitcherLanguageList.displayedChild = 0
                LOADED -> binding.viewSwitcherLanguageList.displayedChild = 1
            }
        })
        model.data.observe(this, Observer {
            adapter.submitList(
                    it.sortedWith(
                            compareByDescending(OcrLanguage::isInstalled).thenBy(OcrLanguage::displayText)
                    )
            )
        })
    }

    private fun confirmDeleteLanguage(language: OcrLanguage, onConfirm: (OcrLanguage) -> Unit) {
        AlertDialog.Builder(this)
                .setTitle(String.format(getString(R.string.delete_language_title), language.displayText))
                .setMessage(String.format(getString(R.string.delete_language_message), language.size / 1024))
                .setCancelable(true)
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .setPositiveButton(R.string.ocr_language_delete) { _, _ -> onConfirm(language) }
                .show()
    }

    private class LanguagesListAdapter(val onClick: (OcrLanguage) -> Unit) : ListAdapter<OcrLanguage, LanguagesListAdapter.ViewHolder>(ITEM_CALLBACK) {

        companion object {
            val ITEM_CALLBACK = object : DiffUtil.ItemCallback<OcrLanguage>() {
                override fun areItemsTheSame(oldItem: OcrLanguage, newItem: OcrLanguage) =
                        oldItem.value == newItem.value

                override fun areContentsTheSame(oldItem: OcrLanguage, newItem: OcrLanguage) =
                        oldItem == newItem
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                ViewHolder(inflate(LayoutInflater.from(parent.context)))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        private inner class ViewHolder(private val binding: ItemListOcrLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(language: OcrLanguage) {
                when {
                    language.isInstalled -> binding.viewFlipper.displayedChild = 2
                    language.isDownloading -> binding.viewFlipper.displayedChild = 1
                    else -> binding.viewFlipper.displayedChild = 0
                }
                binding.textViewLanguage.text = language.displayText
                binding.root.setOnClickListener { onClick(language) }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun getHintDialogId() = -1

    override fun getScreenName() = "Ocr Languages"

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.ocr_language_list_options, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.setOnSearchClickListener {
            binding.toolbar.toolbarContent.visibility = View.GONE
        }
        searchView.setOnCloseListener {
            binding.toolbar.toolbarContent.visibility = View.VISIBLE
            false
        }
        searchView.queryHint = getString(R.string.action_search_languages)
        return true;
    }

    override fun onQueryTextSubmit(query: String) = onQueryTextChange(query)

    override fun onQueryTextChange(newText: String): Boolean {
        val model: LanguageListViewModel by viewModels()
        model.filter(newText)
        return true
    }
}
