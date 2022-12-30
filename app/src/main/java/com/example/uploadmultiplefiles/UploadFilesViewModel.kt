@file:Suppress("DEPRECATION")

package com.example.uploadmultiplefiles

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.uploadmultiplefiles.api.FileUploader
import java.io.File

@Suppress("DEPRECATION")
class UploadFilesViewModel : ViewModel() {

    fun uploadFiles(files: ArrayList<String>) {
        val filesToUpload: Array<File?> = arrayOfNulls(files.size)
        for (i in 0 until files.size) {
            filesToUpload[i] = File(files[i])
        }

        val fileUploader = FileUploader()
        fileUploader.uploadFiles("/", "file", filesToUpload, object :
            FileUploader.FileUploaderCallback {
            @Override
            override fun onError() {

            }

            override fun onFinish(responses: Array<String?>?) {
                for (i in responses!!.indices) {
                    responses[i]
                    responses[i]?.let { Log.e("RESPONSE $i", it) }
                }
            }

            @Override
            override fun onProgressUpdate(currentPercent: Int, totalPercent: Int, fileNumber: Int) {
                Log.e("Progress Status", "$currentPercent $totalPercent $fileNumber")
            }
        })
    }

}