package io.github.willyrafaelfs.speedtest.presentation.component

/** Status izin lokasi yang dibutuhkan Android untuk membaca nama Wi-Fi. */
enum class LocationAccess {
    GRANTED,

    /** Belum diizinkan, tapi dialog izin sistem masih bisa ditampilkan. */
    REQUESTABLE,

    /** User menolak permanen; satu-satunya jalan adalah halaman Setelan aplikasi. */
    BLOCKED,
}
