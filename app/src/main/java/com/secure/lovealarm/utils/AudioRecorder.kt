package com.secure.lovealarm.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    
    fun startRecording(): String? {
        try {
            val audioDir = File(context.filesDir, "alarm_recordings")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            
            outputFile = File(audioDir, "alarm_${System.currentTimeMillis()}.m4a")
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                
                prepare()
                start()
            }
            
            return outputFile?.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            release()
            return null
        }
    }
    
    fun stopRecording(): String? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            outputFile?.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            release()
            null
        }
    }
    
    fun isRecording(): Boolean {
        return try {
            mediaRecorder != null
        } catch (e: Exception) {
            false
        }
    }
    
    private fun release() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
    }
    
    fun cancelRecording() {
        release()
        outputFile?.delete()
        outputFile = null
    }

    /**
     * Copies the recording from app storage to device (Downloads) so the user can keep it.
     * Returns the new URI string (content URI on API 29+, file path on older), or null on failure.
     */
    fun copyRecordingToDevice(sourcePath: String): String? {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fileName = "LoveAlarm_recording_${System.currentTimeMillis()}.m4a"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LoveAlarm")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    it.toString()
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val loveAlarmDir = File(downloadsDir, "LoveAlarm")
                if (!loveAlarmDir.exists()) loveAlarmDir.mkdirs()
                val destFile = File(loveAlarmDir, "LoveAlarm_recording_${System.currentTimeMillis()}.m4a")
                sourceFile.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}



