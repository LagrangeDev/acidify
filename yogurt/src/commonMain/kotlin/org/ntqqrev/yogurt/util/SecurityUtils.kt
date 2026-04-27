package org.ntqqrev.yogurt.util

import kotlinx.io.files.Path
import org.ntqqrev.ktfs.withFs

val isDockerEnv: Boolean by lazy {
    withFs { Path("/.dockerenv").exists }
}