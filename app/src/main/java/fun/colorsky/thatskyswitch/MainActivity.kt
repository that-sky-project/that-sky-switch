package `fun`.colorsky.thatskyswitch

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `fun`.colorsky.thatskyswitch.ui.theme.ThatSkySwitchTheme
import io.github.libxposed.service.XposedService

class MainActivity : ComponentActivity(), App.ServiceStateListener {
    private val serviceState = mutableStateOf<XposedService?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        App.addServiceStateListener(this, notifyNow = true)
        setContent {
            ThatSkySwitchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ServerConfigScreen(
                        service = serviceState.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        App.removeServiceStateListener(this)
        super.onDestroy()
    }

    override fun onServiceStateChanged(service: XposedService?) {
        // 更新 Compose 状态，触发 UI 刷新
        serviceState.value = service
    }
}

private const val DEFAULT_HOST = "live.radiance.thatgamecompany.com"

@Composable
fun ServerConfigScreen(service: XposedService?, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 从远程偏好读取当前值
    val currentHost = try {
        service?.getRemotePreferences("server_config")?.getString("hostname", DEFAULT_HOST) ?: DEFAULT_HOST
    } catch (_: Exception) { DEFAULT_HOST }

    val currentSkipVerify = try {
        service?.getRemotePreferences("verify_config")?.getBoolean("skip", false) ?: false
    } catch (_: Exception) { false }

    var hostname by remember(service) { mutableStateOf(currentHost) }
    var skipVerify by remember(service) { mutableStateOf(currentSkipVerify) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "服务器配置", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = hostname,
            onValueChange = { hostname = it; saved = false },
            label = { Text("服务器地址") },
            placeholder = { Text(DEFAULT_HOST) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 跳过证书验证开关
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "跳过证书验证", style = MaterialTheme.typography.titleMedium)
                Text(text = "启用后将跳过 SSL 证书验证", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = skipVerify,
                onCheckedChange = { newValue ->
                    skipVerify = newValue
                    if (service != null) {
                        service.getRemotePreferences("verify_config")
                            .edit()?.putBoolean("skip", newValue)?.apply()
                        saved = true
                    } else {
                        Toast.makeText(context, "XposedService 未连接", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (service != null) {
                    service.getRemotePreferences("server_config")
                        .edit()?.putString("hostname", hostname)?.apply()
                    saved = true
                } else {
                    Toast.makeText(context, "XposedService 未连接", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存服务器地址")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (saved) {
            Text(text = "✅ 已保存，重启目标应用后生效", color = MaterialTheme.colorScheme.primary)
        }

        if (service == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "⚠️ XposedService 未连接，请检查模块是否激活", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ServerConfigPreview() {
    ThatSkySwitchTheme {
        ServerConfigScreen(service = null)
    }
}
