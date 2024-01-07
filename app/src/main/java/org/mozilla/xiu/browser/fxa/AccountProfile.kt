package org.mozilla.xiu.browser.fxa

import org.mozilla.xiu.browser.R

data class AccountProfile(
    val uid: String? = "00000",
    val email: String? = "null@xiu.com",
    val avatar: Any? = R.drawable.person_circle,
    val displayName: String? = "Xiu@null",
)