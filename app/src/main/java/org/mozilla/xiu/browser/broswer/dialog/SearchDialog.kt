package org.mozilla.xiu.browser.broswer.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import androidx.preference.PreferenceManager
import org.mozilla.xiu.browser.componets.MyDialog
import org.mozilla.xiu.browser.databinding.DiaEngineSelectBinding

class SearchDialog(context: Context) : MyDialog(context) {
    lateinit var binding: DiaEngineSelectBinding

    init {
        onCreate(context)
    }

    fun onCreate(context: Context?) {
        binding = DiaEngineSelectBinding.inflate(LayoutInflater.from(context))
        val prefs = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        val isDiy = prefs?.getBoolean("switch_diy", false)


        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->

            when (findViewById<RadioButton>(checkedId)) {
                binding.radioButton1 -> {
                    prefs?.edit()?.putString(
                        "searchEngine",
                        getContext().getString(org.mozilla.xiu.browser.R.string.baidu)
                    )
                        ?.commit()
                    prefs?.edit()?.putBoolean("switch_diy", false)?.commit()
                }

                binding.radioButton2 -> {
                    prefs?.edit()?.putString(
                        "searchEngine",
                        getContext().getString(org.mozilla.xiu.browser.R.string.google)
                    )
                        ?.commit()
                    prefs?.edit()?.putBoolean("switch_diy", false)?.commit()
                }

                binding.radioButton3 -> {
                    prefs?.edit()?.putString(
                        "searchEngine",
                        getContext().getString(org.mozilla.xiu.browser.R.string.bing)
                    )
                        ?.commit()
                    prefs?.edit()?.putBoolean("switch_diy", false)?.commit()
                }

                binding.radioButton4 -> {
                    prefs?.edit()?.putString(
                        "searchEngine",
                        getContext().getString(org.mozilla.xiu.browser.R.string.sogou)
                    )
                        ?.commit()
                    prefs?.edit()?.putBoolean("switch_diy", false)?.commit()
                }

                binding.radioButton6 -> {
                    prefs?.edit()?.putString(
                        "searchEngine",
                        getContext().getString(org.mozilla.xiu.browser.R.string.sk360)
                    )
                        ?.commit()
                    prefs?.edit()?.putBoolean("switch_diy", false)?.commit()
                }

                binding.radioButton7 -> {
                    prefs?.edit()?.putString(
                        "searchEngine",
                        getContext().getString(org.mozilla.xiu.browser.R.string.wuzhui)
                    )
                        ?.commit()
                    prefs?.edit()?.putBoolean("switch_diy", false)?.commit()
                }

                binding.radioButton8 -> {
                    prefs?.edit()?.putString(
                        "searchEngine",
                        getContext().getString(org.mozilla.xiu.browser.R.string.yandex)
                    )
                        ?.commit()
                    prefs?.edit()?.putBoolean("switch_diy", false)?.commit()
                }

                binding.radioButton9 -> {
                    prefs?.edit()?.putString(
                        "searchEngine",
                        getContext().getString(org.mozilla.xiu.browser.R.string.shenma)
                    )
                        ?.commit()
                    prefs?.edit()?.putBoolean("switch_diy", false)?.commit()
                }
            }
            dismiss()
        }

        if (prefs != null) {
            when (prefs.getString(
                "searchEngine",
                getContext().getString(org.mozilla.xiu.browser.R.string.baidu)
            )) {
                getContext().getString(org.mozilla.xiu.browser.R.string.baidu) -> binding.radioButton1.isChecked =
                    true

                getContext().getString(org.mozilla.xiu.browser.R.string.google) -> binding.radioButton2.isChecked =
                    true

                getContext().getString(org.mozilla.xiu.browser.R.string.bing) -> binding.radioButton3.isChecked =
                    true

                getContext().getString(org.mozilla.xiu.browser.R.string.sogou) -> binding.radioButton4.isChecked =
                    true

                getContext().getString(org.mozilla.xiu.browser.R.string.sk360) -> binding.radioButton6.isChecked =
                    true

                getContext().getString(org.mozilla.xiu.browser.R.string.wuzhui) -> binding.radioButton7.isChecked =
                    true

                getContext().getString(org.mozilla.xiu.browser.R.string.yandex) -> binding.radioButton8.isChecked =
                    true

                getContext().getString(org.mozilla.xiu.browser.R.string.shenma) -> binding.radioButton9.isChecked =
                    true
            }
        }
        if (isDiy == true) {
            binding.radioButton5.visibility = View.VISIBLE
            binding.radioButton5.isChecked = true
        } else
            binding.radioButton5.visibility = View.GONE
        setView(binding.root)
    }


}