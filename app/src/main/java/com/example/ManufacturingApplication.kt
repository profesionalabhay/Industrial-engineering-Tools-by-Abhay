package com.example

import android.app.Application
import com.example.data.ManufacturingRepository
import com.example.data.db.AppDatabase

class ManufacturingApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ManufacturingRepository(database) }
}
