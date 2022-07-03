package com.queatz.plugins

import com.google.gson.Gson
import com.queatz.Secrets
import com.queatz.db.Db
import java.io.File
import java.io.FileNotFoundException

val secrets = try {
    Gson().fromJson(File("./secrets.json").reader(), Secrets::class.java)!!
} catch (exception: FileNotFoundException) {
    System.err.println("The secrets.json file is missing! See Secrets.kt for the data structure.")
    throw exception
}

val db = Db()
