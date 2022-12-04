package com.pihanya.ktlint

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

fun Project.config() = ServiceManager.getService(this, KtlintConfigStorage::class.java)!!
