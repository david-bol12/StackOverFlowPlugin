package com.example

/**
 * Abstracts IDE-specific build error listening.
 *
 * Each IDE has its own build-event interface, so each adapter also
 * implements the IDE's listener interface directly. The [IdeBuildAdapterFactory]
 * picks the right concrete adapter at runtime via PlatformUtils.
 *
 * To add support for a new IDE:
 *   1. Create a new adapter class (e.g. RiderBuildAdapter) that implements
 *      this interface and the IDE's listener interface.
 *   2. Add one branch to [IdeBuildAdapterFactory.create].
 *   3. Register the new listener class in plugin.xml under the appropriate
 *      extension point (see the "ADD NEW IDE" comment there).
 *   — Nothing else needs to change.
 */
interface IdeBuildAdapter {
    /** True when this adapter should handle events in the currently running IDE. */
    val isActive: Boolean
}
