package org.ntqqrev.yogurt.util

import kotlinx.io.files.Path
import org.ntqqrev.yogurt.fs.withFs

val isDockerEnv: Boolean by lazy {
    withFs { exists(Path("/.dockerenv")) }
}