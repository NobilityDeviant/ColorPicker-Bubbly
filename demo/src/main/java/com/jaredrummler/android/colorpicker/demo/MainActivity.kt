/*
 * Copyright (C) 2017 Jared Rummler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaredrummler.android.colorpicker.demo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.jaredrummler.android.colorpicker.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), ColorPickerDialogListener {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_color_picker_dialog) {
            ColorPickerDialog.newBuilder()
                .setShowColorShades(true)
                .setDialogId(DIALOG_ID)
                .setColor(Color.CYAN)
                .setAllowPresets(true)
                .setShowAlphaSlider(true)
                .show(this)
            return true
        } else if (id == R.id.menu_github) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/jaredrummler/ColorPicker")
                    )
                )
            } catch (ignored: ActivityNotFoundException) {
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        Log.d(TAG, "onColorSelected() called with: dialogId = [$dialogId], color = [$color]")
        // We got result from the dialog that is shown when clicking on the icon in the action bar.
        if (dialogId == DIALOG_ID) {
            Toast.makeText(
                this@MainActivity,
                "Selected Color: #" + Integer.toHexString(color),
                Toast.LENGTH_SHORT
            ).show()
            updateColor(color)
        }
    }

    override fun onDialogDismissed(dialogId: Int) {
        Log.d(TAG, "onDialogDismissed() called with: dialogId = [$dialogId]")
    }

    private fun updateColor(color: Int) {
        binding.colorBox.setBackgroundColor(color)
    }

    companion object {
        private const val TAG = "MainActivity"

        // Give your color picker dialog unique IDs if you have multiple dialogs.
        private const val DIALOG_ID = 0
    }
}