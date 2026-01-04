package fr.kevw.kenemimusic

import android.util.Log
import kotlinx.coroutines.*

object PerformanceMonitor {
    private const val TAG = "KenemiPerformance"
    
    fun measureTime(tag: String, operation: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()
            try {
                operation()
            } finally {
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "$tag completed in ${duration}ms")
                
                if (duration > 1000) {
                    Log.w(TAG, "⚠️ $tag is slow (${duration}ms)")
                }
            }
        }
    }
    
    fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        Log.d(TAG, "Memory: $usedMemory/$maxMemory MB")
    }
}

suspend fun <T> withPerformanceMonitoring(
    tag: String,
    operation: suspend () -> T
): T {
    val startTime = System.currentTimeMillis()
    return try {
        operation()
    } finally {
        val duration = System.currentTimeMillis() - startTime
        Log.d("KenemiPerformance", "$tag: ${duration}ms")
    }
}