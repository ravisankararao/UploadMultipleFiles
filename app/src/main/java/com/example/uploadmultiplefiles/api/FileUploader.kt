package com.example.uploadmultiplefiles.api

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException

@Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
class FileUploader {
    var fileUploaderCallback: FileUploaderCallback? = null
    private lateinit var files: Array<File?>
    var uploadIndex = -1
    private var uploadURL = ""
    private var totalFileLength: Long = 0
    private var totalFileUploaded: Long = 0
    private var fileKey = ""
    private val uploadInterface: UploadInterface
    private var authToken = ""
    private lateinit var responses: Array<String?>

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2048
    }

    private interface UploadInterface {
        @Multipart
        @POST
        fun uploadFile(
            @Url url: String?,
            @Part file: MultipartBody.Part?,
            @Header("Authorization") authorization: String?
        ): Call<Any?>?

        @Multipart
        @POST
        fun uploadFile(@Url url: String?, @Part file: MultipartBody.Part?): Call<Any?>?
    }

    interface FileUploaderCallback {
        fun onError()
        fun onFinish(responses: Array<String?>?)
        fun onProgressUpdate(currentPercent: Int, totalPercent: Int, fileNumber: Int)
    }

    inner class PRRequestBody(file: File) : RequestBody() {
        private val mFile: File
        @Override
        override fun contentType(): MediaType? {
            // i want to upload only images
            return "image/*".toMediaTypeOrNull()
        }

        @Override
        @Throws(IOException::class)
        override fun contentLength(): Long {
            return mFile.length()
        }

        @Override
        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            val fileLength: Long = mFile.length()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val inputValue = FileInputStream(mFile)
            var uploaded: Long = 0
            inputValue.use { inputValue ->
                var read: Int
                val handler = Handler(Looper.getMainLooper())
                while (inputValue.read(buffer).also { read = it } != -1) {

                    // update progress on UI thread
                    handler.post(ProgressUpdater(uploaded, fileLength))
                    uploaded += read.toLong()
                    sink.write(buffer, 0, read)
                }
            }
        }

        init {
            mFile = file
        }
    }

    fun uploadFiles(
        url: String,
        fileKey: String,
        files: Array<File?>,
        fileUploaderCallback: FileUploaderCallback?
    ) {
        uploadFiles(url, fileKey, files, fileUploaderCallback, "")
    }

    private fun uploadFiles(
        url: String,
        fileKey: String,
        files: Array<File?>,
        fileUploaderCallback: FileUploaderCallback?,
        authToken: String
    ) {
        this.fileUploaderCallback = fileUploaderCallback
        this.files = files
        uploadIndex = -1
        uploadURL = url
        this.fileKey = fileKey
        this.authToken = authToken
        totalFileUploaded = 0
        totalFileLength = 0
        uploadIndex = -1
        responses = arrayOfNulls(files.size)
        for (i in files.indices) {
            totalFileLength += files[i]!!.length()
        }
        uploadNext()
    }

    private fun uploadNext() {
        if (files.isNotEmpty()) {
            if (uploadIndex != -1) totalFileUploaded += files[uploadIndex]!!.length()
            uploadIndex++
            if (uploadIndex < files.size) {
                uploadSingleFile(uploadIndex)
            } else {
                fileUploaderCallback!!.onFinish(responses)
            }
        } else {
            fileUploaderCallback!!.onFinish(responses)
        }
    }

    private fun uploadSingleFile(index: Int) {
        val fileBody = PRRequestBody(
            files[index]!!
        )
        val filePart: MultipartBody.Part =
            MultipartBody.Part.createFormData(fileKey, files[index]!!.name, fileBody)
        val call: Call<Any?> = if (authToken.isEmpty()) ({
            uploadInterface.uploadFile(uploadURL, filePart)
        }) as Call<Any?> else ({
            uploadInterface.uploadFile(uploadURL, filePart, authToken)
        }) as Call<Any?>
        call.enqueue(object : Callback<Any?> {

            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                if (response.isSuccessful) {
                    val jsonElement: Any? = response.body()
                    responses[index] = jsonElement.toString()
                } else {
                    responses[index] = ""
                }
                uploadNext()
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                fileUploaderCallback!!.onError()
            }
        })
    }

    private inner class ProgressUpdater(private val mUploaded: Long, private val mTotal: Long) :
        Runnable {
        @Override
        override fun run() {
            val currentPercent = (100 * mUploaded / mTotal).toInt()
            val totalPercent = (100 * (totalFileUploaded + mUploaded) / totalFileLength).toInt()
            fileUploaderCallback!!.onProgressUpdate(currentPercent, totalPercent, uploadIndex + 1)
        }
    }

    init {
        uploadInterface = ApiClient.client!!.create(UploadInterface::class.java)
    }
}