package com.wboat.ghosttouchblocker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.wboat.ghosttouchblocker.databinding.ActivityRegionSelectorBinding

class RegionSelectorActivity : Activity() {
    private lateinit var binding: ActivityRegionSelectorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegionSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabConfirm.setOnClickListener {
            binding.regionSelector.selectedRect?.let { rect ->
                setResult(RESULT_OK, Intent().apply {
                    putExtra("left", rect.left)
                    putExtra("top", rect.top)
                    putExtra("right", rect.right)
                    putExtra("bottom", rect.bottom)
                })
                finish()
            }
        }
    }
}
