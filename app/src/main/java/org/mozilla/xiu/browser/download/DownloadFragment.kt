package org.mozilla.xiu.browser.download

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.databinding.FragmentDownloadBinding
import org.mozilla.xiu.browser.utils.scanLocalFiles
import java.io.File


/**
 * A simple [Fragment] subclass.
 * Use the [DownloadFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DownloadFragment : Fragment() {
    private var _binding: FragmentDownloadBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var adapter: DownloadListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentDownloadBinding.inflate(inflater, container, false)
        adapter = DownloadListAdapter {
            val list = DownloadTaskLiveData.getInstance().value?.toMutableList() ?: arrayListOf()
            updateList(adapter, list)
        }
        binding.DownloadRecyclerView.layoutManager = LinearLayoutManager(context);
        binding.DownloadRecyclerView.adapter = adapter
        DownloadTaskLiveData.getInstance().observe(viewLifecycleOwner) {
            val list = it.toMutableList()
            updateList(adapter, list)
        }
        binding.localButton4.setOnClickListener {
            val intent = Intent(context, FileBrowserActivity::class.java)
            val relativePath: String =
                Environment.DIRECTORY_DOWNLOADS + File.separator + ContextCompat.getString(
                    requireContext(),
                    R.string.app_name
                )
            intent.putExtra(
                "path",
                "${Environment.getExternalStorageDirectory()}/$relativePath"
            )
            startActivity(intent)
        }
//        if (activity?.intent?.getBooleanExtra("downloaded", false) == true) {
//            val intent = Intent(context, FileBrowserActivity::class.java)
//            val relativePath: String =
//                Environment.DIRECTORY_DOWNLOADS + File.separator + ContextCompat.getString(
//                    requireContext(),
//                    R.string.app_name
//                )
//            intent.putExtra(
//                "path",
//                "${Environment.getExternalStorageDirectory()}/$relativePath"
//            )
//            startActivity(intent)
//        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateList(adapter, arrayListOf())
    }

    private fun updateList(adapter: DownloadListAdapter, list: MutableList<DownloadTask>) {
        val files = scanLocalFiles(requireContext())
        if (!files.isNullOrEmpty()) {
            val exist = list.map { it.filename }.toSet()
            for (fileEntity in files) {
                if (exist.contains(fileEntity.name)) {
                    continue
                }
                val task = DownloadTask(
                    requireContext(),
                    fileEntity.uri,
                    fileEntity.name
                )
                task.text = fileEntity.time + " " + fileEntity.size
                task.state = 2
                list.add(task)
            }
        }
        adapter.submitList(list)
    }
}