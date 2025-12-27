package com.github.yumelira.yumebox.substore

import com.caoccao.javet.enums.V8AwaitMode
import com.caoccao.javet.interception.logging.JavetStandardConsoleInterceptor
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.options.NodeRuntimeOptions
import dev.oom_wg.purejoy.mlang.MLang
import timber.log.Timber
import java.io.Closeable
import java.io.File

class CaseEngine(backendPort: Int, frontendPort: Int, allowLan: Boolean) : Closeable {
    val host = if (allowLan) "0.0.0.0" else "127.0.0.1"

    private var nodeRuntime: NodeRuntime? = null
    private lateinit var thread: Thread

    @Volatile
    private var shouldAwait = true

    @Volatile
    private var isRunning = false

    private val argv2EnvScript = """
        process.argv.slice(2).forEach(arg => {
          if (arg.startsWith('--SUB_STORE')) {
            const [key, value] = arg.slice(2).split('=');
            if (key && value !== undefined) {
              process.env[key] = value;
            }
          }
        });
    """.trimIndent()

    init {
        try {
            SubStorePaths.ensureStructure()
            val nodeRuntimeOptions = NodeRuntimeOptions()
            nodeRuntimeOptions.setConsoleArguments(
                arrayOf(
                    "--allow-fs-read",
                    "--allow-fs-write",
                    "--SUB_STORE_FRONTEND_HOST=$host",
                    "--SUB_STORE_FRONTEND_PORT=$frontendPort",
                    "--SUB_STORE_FRONTEND_PATH=${SubStorePaths.frontendDir.absolutePath}",
                    "--SUB_STORE_BACKEND_API_HOST=$host",
                    "--SUB_STORE_BACKEND_API_PORT=$backendPort",
                    "--SUB_STORE_DATA_BASE_PATH=${SubStorePaths.dataDir.absolutePath}",
                )
            )

            nodeRuntime = V8Host.getNodeInstance().createV8Runtime(nodeRuntimeOptions)

            val javetStandardConsoleInterceptor = JavetStandardConsoleInterceptor(nodeRuntime)
            javetStandardConsoleInterceptor.register(nodeRuntime!!.globalObject)

            nodeRuntime!!.allowEval(true)
            nodeRuntime!!.getExecutor(argv2EnvScript).executeVoid()
        } catch (e: Exception) {
            Timber.e(e, MLang.Feature.SubStore.CaseEngineInitFailed)
        }
    }

    fun startServer() {
        if (isRunning) return
        isRunning = true
        shouldAwait = true

        val codeFile: File = SubStorePaths.backendBundle

        thread = Thread {
            try {
                val workingDir = SubStorePaths.workingDir.path
                nodeRuntime!!.getExecutor("process.chdir('$workingDir')").executeVoid()

                nodeRuntime!!.getExecutor(codeFile).executeVoid()
                while (shouldAwait) {
                    nodeRuntime!!.await(V8AwaitMode.RunOnce)
                }
            } catch (_: InterruptedException) {
            } catch (e: Exception) {
                Timber.e(e, MLang.Feature.SubStore.CaseEngineStartFailed.format(e.message ?: ""))
            } finally {
                cleanup()
            }
        }.apply {
            name = "CaseEngine-Worker"
            start()
        }
    }

    fun isInitialized(): Boolean = nodeRuntime != null

    override fun close() {
        stopServer()
    }

    fun stopServer() {
        if (!isRunning) return

        try {
            shouldAwait = false
            nodeRuntime?.terminateExecution()

            if (::thread.isInitialized && thread.isAlive) {
                thread.interrupt()
                thread.join(5000)
            }
        } catch (e: Exception) {
            Timber.e(e, MLang.Feature.SubStore.CaseEngineStopFailed)
        }
    }

    private fun cleanup() {
        isRunning = false
        runCatching {
            nodeRuntime?.let {
                if (!it.isClosed) {
                    it.isStopping = true
                    it.close()
                }
            }
            nodeRuntime = null
        }.onFailure { e ->
            Timber.e(e, MLang.Feature.SubStore.CaseEngineStopFailed.format(e.message ?: ""))
        }
    }
}
