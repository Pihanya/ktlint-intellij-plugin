package com.pihanya.ktlint.config

import com.intellij.openapi.components.*

@Service(Service.Level.PROJECT)
@State(name = "KtlintGlobalSettings", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
internal class KtlintGlobalSettings : SimplePersistentStateComponent<KtlintGlobalSettings.State>(State()) {

    class State : BaseState() {

        var stateVersion by property(0)
    }
}
