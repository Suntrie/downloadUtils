package com.repoMiner

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.*
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class DownloadUtils {

    companion object {

        fun downloadFromUrl(downloadUrl: String,
                            fileLogger: Logger): ByteArray? {

            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(60 * 60 * 2, TimeUnit.SECONDS).build()

            val request = Request.Builder().url(downloadUrl).build()

            lateinit var response: Response;

            fileLogger.log(Level.INFO, "Start downloading...")

            response = client.newCall(request).execute()

            if (!response.isSuccessful) {

                (response.body() as ResponseBody).close()

                fileLogger.log(Level.SEVERE, "Request failed with reason: " + response)

                return null
            } else {

                val bytesInputStream = (response.body()!! as ResponseBody).byteStream()
                val bytesOutputStream = ByteArrayOutputStream()

                val byteBatchBuffer = ByteArray(16384)
                var nRead: Int = bytesInputStream.read(byteBatchBuffer, 0, byteBatchBuffer.size)

                while (nRead != -1) {

                    try {
                        bytesOutputStream.write(byteBatchBuffer, 0, nRead)
                        nRead = bytesInputStream.read(byteBatchBuffer, 0, byteBatchBuffer.size)
                    } catch (e: IOException) {

                        clearRemoteReadResources(bytesInputStream, response)

                        if (e is SocketTimeoutException) {
                            fileLogger.log(Level.SEVERE,
                                    "This repository is too big to be obtained " +
                                            "during 2h and will be excluded because of it")
                            return null
                        } else {
                            fileLogger.log(Level.SEVERE, "Connection was aborted due to cause: ${e.message}")
                            throw e
                        }
                    }
                }

                clearRemoteReadResources(bytesInputStream, response)

                return bytesOutputStream.toByteArray()
            }
        }

        private fun clearRemoteReadResources(bytesInputStream: InputStream,
                                             response: Response) {
            bytesInputStream.close()
            (response.body() as ResponseBody).close()
        }

        fun saveDownloadedDataOnDisk(data: ByteArray,
                                     path: String,
                                     fileLogger: Logger) {

            val fileOutputStream = FileOutputStream(path)
            fileOutputStream.write(data)
            fileOutputStream.close()
            fileLogger.log(Level.INFO, "GitProject's files has been stored on disk.")
        }


        fun checkAndCreateRepoFSStuffIfAbsent(path: String): Boolean {
            val file = File(path)
            if (!file.exists()) {
                if (!file.parentFile.exists()) {

                    if (!file.parentFile.mkdirs()) {
                        println("Failed to create directory ${file.parentFile}!")
                        return false
                    }

                }

                if (!file.createNewFile()) {
                    println("Failed to create file!")
                    return false
                } else {
                    println("File ${file.toPath()} is created!")
                    return true
                }

            } else
                return true
        }

    }
}