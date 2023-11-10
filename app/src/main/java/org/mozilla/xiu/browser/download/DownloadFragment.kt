package org.mozilla.xiu.browser.download

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.mozilla.xiu.browser.databinding.FragmentDownloadBinding


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentDownloadBinding.inflate(inflater, container, false)
        var adapter = DownloadListAdapter()
        binding.DownloadRecyclerView.layoutManager = LinearLayoutManager(context);
        binding.DownloadRecyclerView.adapter = adapter
        DownloadTaskLiveData.getInstance().observe(viewLifecycleOwner) {
            adapter.submitList(it.toList())
        }
        binding.localButton4.setOnClickListener {
            val intent = Intent(context, FileBrowserActivity::class.java)
            val relativePath: String = Environment.DIRECTORY_DOWNLOADS
            intent.putExtra(
                "path",
                "${Environment.getExternalStorageDirectory()}/$relativePath"
            )
            startActivity(intent)
        }
        return binding.root
    }


}