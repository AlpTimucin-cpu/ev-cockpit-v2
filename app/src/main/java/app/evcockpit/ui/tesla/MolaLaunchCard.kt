package app.evcockpit.ui.tesla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.evcockpit.integration.MolaLinkState
import app.evcockpit.integration.MolaStatus
import app.evcockpit.ui.CockpitTheme

/**
 * A card in EV Cockpit, NEVER inside Mola. Only public Android launch/settings/URL actions.
 * All callbacks are owner/ViewModel actions, not hidden application APIs.
 */
@Composable
fun MolaLaunchCard(
    state: MolaLinkState,
    onOpen: () -> Unit,
    onInstallGuide: () -> Unit,
    onAppSettings: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mola bağlantısı") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(state.message)
                Text(
                    "Mola'nın tasarımı, verileri ve izinleri değiştirilmez. " +
                        "Telefon uygulaması olarak açılır; araç ekranı için sonuç DOĞRULANMADI."
                )
                when (state.status) {
                    MolaStatus.READY -> {
                        OutlinedButton(onClick = onOpen,
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = TeslaTokens.touchTarget)) {
                            Text("Mola'yı aç")
                        }
                    }
                    MolaStatus.NOT_INSTALLED -> {
                        Text("Mola kurulu değil")
                        OutlinedButton(onClick = onInstallGuide,
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = TeslaTokens.touchTarget)) {
                            Text("Resmî kurulum rehberini aç")
                        }
                    }
                    MolaStatus.CANNOT_LAUNCH, MolaStatus.ERROR -> {
                        if (state.packageName != null) OutlinedButton(
                            onClick = onAppSettings,
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = TeslaTokens.touchTarget),
                        ) { Text("Android uygulama ayarlarını aç") }
                        if (state.packageName == null) OutlinedButton(
                            onClick = onInstallGuide,
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = TeslaTokens.touchTarget),
                        ) { Text("Resmî yayın sayfasını aç") }
                    }
                    MolaStatus.CHECKING -> Text("Yükleniyor…")
                }
                OutlinedButton(onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = TeslaTokens.touchTarget)) {
                    Text("Kurulumu yeniden kontrol et")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("EV Cockpit'e dön") }
        },
    )
}

@Preview(name = "Mola kurulu / DEMO", showBackground = true)
@Composable private fun MolaReadyPreview() {
    CockpitTheme(dark = true) {
        MolaLaunchCard(
            MolaLinkState(MolaStatus.READY,
                "me.app.fermataX.auto.mola", "Kurulu • önizleme / DEMO"),
            {}, {}, {}, {}, {},
        )
    }
}

@Preview(name = "Mola kurulu değil / DEMO", showBackground = true)
@Composable private fun MolaMissingPreview() {
    CockpitTheme(dark = true) {
        MolaLaunchCard(
            MolaLinkState(MolaStatus.NOT_INSTALLED,
                message = "Mola kurulu değil • önizleme / DEMO"),
            {}, {}, {}, {}, {},
        )
    }
}

@Preview(name = "Mola açılamadı / DEMO", showBackground = true)
@Composable private fun MolaErrorPreview() {
    CockpitTheme(dark = true) {
        MolaLaunchCard(
            MolaLinkState(MolaStatus.ERROR,
                packageName = "me.app.fermataX.auto.mola",
                message = "Mola açılamadı • önizleme / DEMO"),
            {}, {}, {}, {}, {},
        )
    }
}
